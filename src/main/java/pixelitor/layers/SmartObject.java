/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.ImageSource;
import pixelitor.Views;
import pixelitor.compactions.Flip;
import pixelitor.filters.Filter;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.history.*;
import pixelitor.io.FileChoosers;
import pixelitor.io.IO;
import pixelitor.utils.*;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static pixelitor.utils.Threads.onEDT;

/**
 * A "smart object" that contains an embedded composition and allows "smart filters".
 * The cached result image behaves like the image of a regular {@link ImageLayer}.
 */
public class SmartObject extends ImageLayer {
    @Serial
    private static final long serialVersionUID = 8594248957749192719L;

    private static final String NAME_PREFIX = "smart ";
    private Composition content;

    // Only used for a deserialization check.
    // The field is not present in very old pxc versions.
    private final boolean newVersion = true;

    // Null if the content is not linked.
    // This is the same object as the content's file field, but it's not transient.
    private File linkedContentFile;
    private transient long linkedContentFileTime;

    // "Legacy" fields from Pixelitor 4.3.0, they are only
    // used for automatic migration of old pxc files
    private boolean smartFilterIsVisible = true;
    private List<Filter> smartFilters = new ArrayList<>();

    // the real list of smart filters
    private List<SmartFilter> filters = new ArrayList<>();

    private AffineTransform contentTransform;

    private transient boolean imageNeedsRefresh = false;

    // constructor for converting a layer into a smart object
    public SmartObject(Layer layer) {
        super(layer.getComp(), NAME_PREFIX + layer.getName());

        setContent(Composition.createEmpty(comp.getCanvasWidth(), comp.getCanvasHeight(), comp.getMode()));
        // the mask stays outside the content, and will become the mask of the smart object
        Layer contentLayer = layer.duplicate(true, false);
        contentLayer.setName("original content", false);
        contentLayer.setComp(content);
        content.addLayerInInitMode(contentLayer);
        content.setName(getName());
        content.createDebugName();
        copyBlendingFrom(layer);

        recalculateImage(false);

        assert checkConsistency();
    }

    // constructor for creating a smart object with a given content
    public SmartObject(Composition parent, Composition content) {
        super(parent, "Smart " + content.getName());
        setContent(content);
        recalculateImage(false);

        assert checkConsistency();
    }

    // constructor called for "Add Linked"
    public SmartObject(File file, Composition parent, Composition content) {
        super(parent, file.getName());
        linkedContentFile = file;
        updateLinkedContentTime();
        setContent(content);
        recalculateImage(false);

        assert checkConsistency();
    }

    // The constructor used for duplication (same composition).
    private SmartObject(SmartObject orig, String name, boolean deepCopy) {
        super(orig.comp, name);
        if (deepCopy) {
            setContent(orig.content.copy(false, true));
        } else {
            setContent(orig.getContent());
        }
        image = orig.image;

        for (SmartFilter origFilter : orig.filters) {
            SmartFilter copy = (SmartFilter) origFilter.duplicate(false, true);
            copy.setSmartObject(this);
            addSmartFilter(copy, false, false);
        }

        linkedContentFile = orig.linkedContentFile;
        linkedContentFileTime = orig.linkedContentFileTime;
        if (orig.contentTransform != null) {
            contentTransform = new AffineTransform(orig.contentTransform);
        }
        forceTranslation(orig.getTx(), orig.getTy());

        assert checkConsistency();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (!newVersion) {
            // if the pxc was saved with an old version,
            // then assume smart filter visibility
            smartFilterIsVisible = true;
        }
        imageNeedsRefresh = true;

        // more initialization happens later in afterDeserialization()
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        Composition backup = content;
        if (isContentLinked()) {
            // if the content is linked, then don't write it
            content = null;
        }

        out.defaultWriteObject();

        content = backup;
    }

    public void afterDeserialization() {
        if (isContentLinked()) {
            if (linkedContentFile.exists()) {
                updateLinkedContentTime();
                // also read the content
                assert content == null;
                setContent(IO.loadCompSync(linkedContentFile));
            } else { // linked file not found
                // Set a transparent image as content to avoid all sorts of errors.
                // It will be replaced if the content is found later.
                setContent(Composition.createTransparent(
                    comp.getCanvasWidth(), comp.getCanvasHeight()));

                EventQueue.invokeLater(this::handleMissingContent);
            }
        } else {
            assert content != null;
            content.addOwner(this);
        }

        // Migration from 4.3.0 serialization format.
        // It's done here, to make sure that even
        // linked contents are not null.
        migratePXCFormat();

        // has to be done after the migration
        for (SmartFilter filter : filters) {
            filter.updateOptions(this);
        }

        recalculateImage(false);

        assert checkConsistency();
    }

    private void migratePXCFormat() {
        if (filters == null) { // new field, will be null in old pxc files
            filters = new ArrayList<>();
            if (smartFilters.size() > 1) {
                // there can be only one smart filter in the old pxc format
                throw new IllegalStateException("# filters = " + smartFilters.size());
            }
            for (Filter filter : smartFilters) {
                SmartFilter newFilter = new SmartFilter(filter, content, this);
                newFilter.setVisible(smartFilterIsVisible);
                filters.add(newFilter);
            }
            smartFilters = null; // no longer needed
        }
    }

    private void handleMissingContent() {
        assert Threads.calledOnEDT();

        String title = linkedContentFile.getName() + " not found.";
        String msg = "<html>The linked file <b>" + linkedContentFile.getAbsolutePath() +
                     "</b> was not found.<br>You can search for it or use a transparent image.";
        boolean search = Dialogs.showOKCancelDialog(msg, title,
            new String[]{"Search...", "Use Transparent Image"}, 0, JOptionPane.ERROR_MESSAGE);

        File newFile = null;
        if (search) {
            newFile = FileChoosers.getSupportedOpenFile();
        }
        if (newFile != null) { // file found
            linkedContentFile = newFile;
            updateLinkedContentTime();

            IO.loadCompAsync(linkedContentFile)
                .thenAcceptAsync(loadedComp -> {
                    setContent(loadedComp);
                    recalculateImage(true);
                    comp.update();
                }, onEDT);
        } else {
            // give up and use the previously created transparent image
            linkedContentFile = null;
        }
    }

    @Override
    boolean serializeImage() {
        return false;
    }

    public void recalculateImage(boolean updateIcon) {
        recalculateImage(comp.getCanvasWidth(), comp.getCanvasHeight(), updateIcon);
    }

    private void recalculateImage(int targetWidth, int targetHeight, boolean updateIcon) {
//        resetImageFromContent(targetWidth, targetHeight);

        int numFilters = filters.size();
        if (numFilters > 0) {
            image = filters.get(numFilters - 1).getImage();
        } else {
            image = content.getCompositeImage();
        }
        if (updateIcon) {
            updateIconImage();
        }
        imageNeedsRefresh = false;
    }

    private void resetImageFromContent() {
        resetImageFromContent(comp.getCanvasWidth(), comp.getCanvasHeight());
    }

    private void resetImageFromContent(int targetWidth, int targetHeight) {
        image = content.getCompositeImage();
        boolean imageConverted = false;
        if (contentTransform != null) {
            image = ImageUtils.applyTransform(image, contentTransform, targetWidth, targetHeight);
            imageConverted = true;
        }
        if (!imageConverted && image.isAlphaPremultiplied()) {
            // image layer images should not be premultiplied
            image = ImageUtils.copyTo(BufferedImage.TYPE_INT_ARGB, image);
            imageConverted = true;
        }
        if (!imageConverted && ImageUtils.isSubImage(image)) {
            image = ImageUtils.copySubImage(image);
            imageConverted = true;
        }
    }

    public void invalidateImageCache() {
        imageNeedsRefresh = true;
    }

    /**
     * Ensures that all parents will reflect the changes in this
     * smart object when they are repainted.
     */
    public void propagateContentChanges(Composition content, boolean force) {
        // the reference might have been changed during editing
        setContent(content);

        if (!content.isDirty() && !force) {
            return;
        }

        invalidateImageCache();
        if (!filters.isEmpty()) {
            filters.get(0).invalidateChain();
        }

        comp.smartObjectChanged(isContentLinked());
    }

    @Override
    public BufferedImage getVisibleImage() {
        if (imageNeedsRefresh) {
            recalculateImage(true);
        }
        return super.getVisibleImage();
    }

    @Override
    protected boolean isSmartObject() {
        return true;
    }

    @Override
    public boolean isRasterizable() {
        return true;
    }

    @Override
    protected String getRasterizedName() {
        return Utils.removePrefix(name, NAME_PREFIX);
    }

    @Override
    protected Layer createTypeSpecificDuplicate(String duplicateName) {
        return new SmartObject(this, duplicateName, true);
    }

    void addSmartObjectSpecificItems(JPopupMenu popup) {
        popup.add(new PAction("Edit Contents") {
            @Override
            protected void onClick() {
                edit();
            }
        });
        popup.add(new PAction("Clone") {
            @Override
            protected void onClick() {
                comp.shallowDuplicate(SmartObject.this);
            }
        });
        if (isContentLinked()) {
            popup.add(new PAction("Embed Contents") {
                @Override
                protected void onClick() {
                    embedLinkedContent();
                }
            });
            popup.add(new PAction("Reload Contents") {
                @Override
                protected void onClick() {
                    reloadLinkedContent();
                }
            });
        }
        if (Filter.copiedSmartFilter != null) {
            popup.add(new PAction("Paste " + Filter.copiedSmartFilter.getName()) {
                @Override
                protected void onClick() {
                    // copy again, because it could be pasted multiple times
                    pasteSmartFilter(Filter.copiedSmartFilter.copy());
                }
            });
        }
    }

    @Override
    public void edit() {
        View contentView = content.getView();
        if (contentView == null) {
            Views.addAsNewComp(content);
            content.setDirty(false);
        } else {
            Views.activate(contentView);
        }
    }

    public void forAllNestedSmartObjects(Consumer<SmartObject> action) {
        assert checkInvariant();

        action.accept(this);
        content.forAllNestedSmartObjects(action);
    }

    public boolean hasSmartFilters() {
        return !filters.isEmpty();
    }

    public int getNumSmartFilters() {
        return filters.size();
    }

    public SmartFilter getSmartFilter(int index) {
        return filters.get(index);
    }

    private void runAndAddSmartFilter(Filter newFilter) {
        boolean filterDialogAccepted = startFilter(newFilter, false);
        if (filterDialogAccepted) {
            addSmartFilter(newFilter, true, false);
        }
    }

    private void pasteSmartFilter(Filter newFilter) {
        runAndAddSmartFilter(newFilter);
    }

    public void tryAddingSmartFilter(Filter filter) {
        // adds a smart filter, and triggers an edit,
        // but if the user cancels the first edit,
        // then the filter is removed.
    }

    public void addSmartFilter(Filter filter, boolean addToHistory, boolean updateImage) {
        addSmartFilter(new SmartFilter(filter, content, this), addToHistory, updateImage);
    }

    public void addSmartFilter(SmartFilter newFilter, boolean addToHistory, boolean updateImage) {
        int numFilters = filters.size();
        if (numFilters != 0) {
            SmartFilter last = filters.get(numFilters - 1);
            newFilter.setImageSource(last);
            last.setNext(newFilter);
        }

        filters.add(newFilter);

        if (addToHistory) {
            History.add(new NewSmartFilterEdit(this, newFilter));
        }

        if (updateImage) {
            recalculateImage(true);
            comp.update();
        }

        updateSmartFilterUI();
        assert checkConsistency();
    }

    public void insertSmartFilter(SmartFilter filter, int index) {
        SmartFilter previous = null;
        if (index > 0) {
            previous = filters.get(index - 1);
        }

        int numFilters = filters.size();
        SmartFilter next = null;
        if (index < numFilters) {
            next = filters.get(index);
        }

        filters.add(index, filter);

        if (previous != null) {
            previous.setNext(filter);
            filter.setImageSource(previous);
        } else {
            filter.setImageSource(content);
        }

        if (next != null) {
            filter.setNext(next);
            next.setImageSource(filter);
            next.invalidateChain();
        }

        recalculateImage(true);
        comp.update();
        updateSmartFilterUI();
        assert checkConsistency();
    }

    public void deleteSmartFilter(SmartFilter filter, boolean addToHistory) {
        int numFilters = filters.size();
        int index = -1;
        for (int i = 0; i < numFilters; i++) {
            if (filters.get(i) == filter) {
                index = i;
                SmartFilter previous = null;
                SmartFilter next = null;
                if (i > 0) {
                    previous = filters.get(i - 1);
                }
                if (i < numFilters - 1) {
                    next = filters.get(i + 1);
                }
                if (previous != null) {
                    previous.setNext(next);
                }
                if (next != null) {
                    next.invalidateChain();
                    if (previous != null) {
                        next.setImageSource(previous);
                    } else {
                        next.setImageSource(content);
                    }
                }
                filters.remove(i);
                break;
            }
        }

        if (index == -1) {
            throw new IllegalStateException(filter.getName() + " not found in " + getName());
        }

        if (addToHistory) {
            History.add(new DeleteSmartFilterEdit(this, filter, index));
        }

        recalculateImage(true);
        comp.update();
        updateSmartFilterUI();
        assert checkConsistency();
    }

    private void updateSmartFilterUI() {
        if (ui == null) { // in some unit tests
            return;
        }
        ui.updateSmartFilterPanel();
        EventQueue.invokeLater(this::revalidateUI);
    }

    private void revalidateUI() {
        LayersPanel layersPanel = comp.getView().getLayersPanel();
        if (layersPanel != null) { // null in unit tests
            layersPanel.revalidate();
        }
    }

    @Override
    public BufferedImage getCanvasSizedSubImage() {
        // workaround for moved layers
        BufferedImage img = ImageUtils.createSysCompatibleImage(comp.getCanvas());
        Graphics2D g = img.createGraphics();

        // don't call applyLayer, because the mask should NOT be considered
        setupDrawingComposite(g, true);
        paintLayerOnGraphics(g, true);

        g.dispose();
        return img;
    }

    public View getParentView() {
        if (isContentOpen()) {
            return content.getView();
        }
        return comp.getParentView();
    }

    public CompletableFuture<Composition> checkForAutoReload() {
        assert !isContentOpen();
        assert checkInvariant();

        if (isContentLinked()) {
            long newLinkedContentFileTime = linkedContentFile.lastModified();
            if (newLinkedContentFileTime > linkedContentFileTime) {
                linkedContentFileTime = newLinkedContentFileTime;

                Views.activate(getParentView());
                boolean reload = Messages.reloadFileQuestion(linkedContentFile);
                if (reload) {
                    // if this content is reloaded, then return because
                    // the nested smart objects don't have to be checked
                    return reloadLinkedContent();
                }
            }
        }
        // also check recursively deeper
        return content.checkForAutoReload();
    }

    private CompletableFuture<Composition> reloadLinkedContent() {
        return reloadContent(linkedContentFile);
    }

    private CompletableFuture<Composition> reloadContent(File file) {
        return IO.loadCompAsync(file)
            .thenApplyAsync(loaded -> {
                setContent(loaded);
                recalculateImage(true);

                // only a grandparent composition might be opened
                propagateContentChanges(loaded, true);
                getParentView().repaint();
                return loaded;
            }, onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    public Composition getContent() {
        assert checkInvariant();
        return content;
    }

    public void setContent(Composition content) {
        if (this.content == content) {
            return;
        }
        this.content = content;
        content.addOwner(this);

        // if there are smart filters, the first one references the content
        if (filters != null) { // this check is required by the migration support
            if (!filters.isEmpty()) {
                SmartFilter first = filters.get(0);
                first.setImageSource(content);
                first.invalidateChain();
            }
        }
        assert checkConsistency();
    }

    public boolean isContentOpen() {
        return content.isOpen();
    }

    public boolean isContentLinked() {
        return linkedContentFile != null;
    }

    public void setLinkedContentFile(File file) {
        this.linkedContentFile = file;
        updateLinkedContentTime();
    }

    private void updateLinkedContentTime() {
        linkedContentFileTime = linkedContentFile.lastModified();
    }

    private void embedLinkedContent() {
        String path = linkedContentFile.getAbsolutePath();
        linkedContentFile = null;
        Messages.showInfo("Embedded Content",
            "<html>The file <b>" + path + "</b> isn't used anymore.");
    }

    /**
     * Returns the composition that saves the contents of this smart object to its file
     */
    public Composition getSavingComp() {
        SmartObject so = this;
        while (true) {
            if (so.isContentLinked()) {
                return so.getContent();
            }
            Composition parent = so.getComp();
            if (parent.isSmartObjectContent()) {
                // assumes that all owners are in the same comp
                so = parent.getOwners().get(0);
            } else {
                return parent;
            }
        }
    }

    @Override
    public CompletableFuture<Void> resize(Dimension newSize) {
        double sx = newSize.getWidth() / comp.getCanvasWidth();
        double sy = newSize.getHeight() / comp.getCanvasHeight();

        setTranslation((int) (getTx() * sx), (int) (getTy() * sy));
        AffineTransform newScaling = AffineTransform.getScaleInstance(sx, sy);
        if (contentTransform == null) {
            contentTransform = newScaling;
        } else {
            contentTransform.concatenate(newScaling);
        }
        return CompletableFuture.runAsync(() ->
            recalculateImage(newSize.width, newSize.height, false));
    }

    @Override
    public void flip(Flip.Direction direction) {
        AffineTransform flipTransform = direction.createImageTransform(image);
        if (contentTransform == null) {
            contentTransform = flipTransform;
        } else {
            contentTransform.concatenate(flipTransform);
        }
        recalculateImage(image.getWidth(), image.getHeight(), false);
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        AffineTransform rotation = angle.createImageTransform(image);
        if (contentTransform == null) {
            contentTransform = rotation;
        } else {
            contentTransform.concatenate(rotation);
        }
        recalculateImage(image.getWidth(), image.getHeight(), false);
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTx, int oldTy) {
        // a smart object never enlarges the image
        return new ContentLayerMoveEdit(this, null, oldTx, oldTy);
    }

    @Override
    public void setTranslation(int x, int y) {
        // positive translation values are allowed for smart objects
        forceTranslation(x, y);
    }

    @Override
    public BufferedImage getCanvasSizedVisibleImage() {
        int tx = getTx();
        int ty = getTy();
        BufferedImage visibleImage = getVisibleImage();
        if (tx == 0 && ty == 0) {
            assert visibleImage.getWidth() == comp.getCanvasWidth()
                : "visible width = " + visibleImage.getWidth() + ", canvas width = " + comp.getCanvasWidth();
            assert visibleImage.getHeight() == comp.getCanvasHeight()
                : "visible height = " + visibleImage.getHeight() + ", canvas height = " + comp.getCanvasHeight();
            return visibleImage;
        }

        // the image of a moved layer might not cover the canvas,
        // therefore (unlike in the superclass) here we can't use subimage
        BufferedImage img = ImageUtils.createSysCompatibleImage(comp.getCanvas());
        Graphics2D g = img.createGraphics();
        g.drawImage(visibleImage, tx, ty, null);
        g.dispose();

        return img;
    }

    public SmartObject shallowDuplicate() {
        SmartObject duplicate = new SmartObject(this, getName() + " clone", false);
        return duplicate;
    }

    public void moveUp(SmartFilter smartFilter) {
        int index = filters.indexOf(smartFilter);
        if (index == filters.size() - 1) {
            return; // already the last filter
        }
        swapSmartFilters(index, index + 1, "Move " + smartFilter.getName() + " Up");
    }

    public void moveDown(SmartFilter smartFilter) {
        int index = filters.indexOf(smartFilter);
        if (index == 0) {
            return; // already the first filter
        }
        swapSmartFilters(index - 1, index, "Move " + smartFilter.getName() + " Down");
    }

    public void swapSmartFilters(int indexA, int indexB, String editName) {
        assert indexB == indexA + 1;
        SmartFilter filterA = filters.get(indexA);
        SmartFilter filterB = filters.get(indexB);

        // handle the filter bellow them
        SmartFilter bellow = null;
        if (indexA != 0) {
            bellow = filters.get(indexA - 1);
            assert bellow.getNext() == filterA;
            bellow.setNext(filterB);
        }

        // handle the swap
        assert filterA.getNext() == filterB;
        assert filterB.getImageSource() == filterA;
        ImageSource origSource = filterA.getImageSource();
        SmartFilter above = filterB.getNext(); // keep the reference
        filterB.setNext(filterA);
        filterA.setImageSource(filterB);
        filterB.setImageSource(origSource);
        Collections.swap(filters, indexA, indexB);

        // handle the filter above them
        if (above != null) {
            filterA.setNext(above);
            above.setImageSource(filterA);
        } else {
            filterA.setNext(null);
        }
        assert checkConsistency();

        if (editName != null) {
            History.add(new SwapSmartFiltersEdit(editName, this, indexA, indexB));
        }

        // update the GUI
        updateSmartFilterUI();

        // update the image
        SmartFilter lowestChanged = bellow != null ? bellow : filterB;
        lowestChanged.invalidateChain();
        recalculateImage(false);
        comp.update();
    }

    public boolean containsSmartFilter(SmartFilter filter) {
        return filters.contains(filter);
    }

    @Override
    public Drawable getActiveDrawable() {
        return getActiveMask();
    }

    @Override
    public LayerMask getActiveMask() {
        if (isMaskEditing()) {
            return getMask();
        }
        SmartFilter sf = getSelectedSmartFilter();
        if (sf != null && sf.isMaskEditing()) {
            return sf.getMask();
        }
        return null;
    }

    @Override
    public boolean isEditingAnyMask() {
        return getActiveMask() != null;
    }

    @Override
    public void setMaskEditing(boolean newValue) {
        super.setMaskEditing(newValue);
        if (newValue) {
            for (SmartFilter filter : filters) {
                filter.setMaskEditing(false);
            }
        }
    }

    public void setFilterMaskEditing(SmartFilter edited) {
        setMaskEditing(false);
        for (SmartFilter filter : filters) {
            if (filter != edited) {
                filter.setMaskEditing(false);
            }
        }
    }

    public void editSelectedSmartFilter() {
        if (filters.isEmpty()) {
            Messages.showInfo("No Smart Filters",
                "<html>There are no smart filters in the smart object <b>" + getName() + "</>.");
            return;
        }

        SmartFilter selected = getSelectedSmartFilter();
        if (selected != null) {
            selected.edit();
        } else { // the smart object as a whole is selected
            // edit the last smart filter
            filters.get(filters.size() - 1).edit();
        }
    }

    private SmartFilter getSelectedSmartFilter() {
        for (SmartFilter filter : filters) {
            if (filter.isEditingTarget()) {
                return filter;
            }
        }
        return null;
    }

    public boolean checkConsistency() {
        if (!content.getOwners().contains(this)) {
            throw new AssertionError(getName() + " not owner of its content");
        }
        if (filters != null) { // pxc might not be migrated yet
            for (SmartFilter filter : filters) {
                if (!filter.checkConsistency()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean checkInvariant() {
        if (!content.isSmartObjectContent()) {
            throw new IllegalStateException("content of %s (%s) is broken"
                .formatted(getName(), content.getDebugName()));
        }
        return true;
    }

    @Override
    public String getTypeString() {
        return "Smart Object";
    }

    public String debugSmartFilters() {
        return "Filters of " + getName() + ":\n"
               + filters.stream()
                   .map(SmartFilter::toString)
                   .collect(Collectors.joining("\n"));
    }

    public void debugAllImages() {
        BufferedImage soImage = image;
        BufferedImage contentImage = content.getCompositeImage();
//        BufferedImage calcContentImage = content.calculateCompositeImage();

        Debug.debugImage(soImage, "soImage");
        Debug.debugImage(contentImage, "contentImage");
//        Debug.debugImage(calcContentImage, "calcContentImage");
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        boolean linked = isContentLinked();
        node.addBoolean("linked", linked);
        if (linked) {
            node.addString("link file", linkedContentFile.getAbsolutePath());
        }
        node.add(content.createDebugNode("content"));

        for (SmartFilter filter : filters) {
            node.add(filter.createDebugNode("smart filter"));
        }

        return node;
    }
}
