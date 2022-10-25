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

import pixelitor.AppContext;
import pixelitor.Composition;
import pixelitor.CopyType;
import pixelitor.Views;
import pixelitor.compactions.Flip;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.PAction;
import pixelitor.history.*;
import pixelitor.io.FileChoosers;
import pixelitor.io.IO;
import pixelitor.utils.Messages;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.Threads;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static pixelitor.layers.LayerGUILayout.thumbSize;
import static pixelitor.utils.ImageUtils.createThumbnail;
import static pixelitor.utils.Threads.onEDT;

/**
 * A "smart object" that contains an embedded composition and allows "smart filters".
 * The cached result image behaves like the image of a regular {@link ImageLayer}.
 */
public class SmartObject extends CompositeLayer {
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

    private ImageSource baseSource;

    // transformer in old pxc files
    private AffineTransform contentTransform;

    // transformer in new pxc files
    private ImageTransformer imageTransformer;

    private transient boolean imageNeedsRefresh = false;

    // the cached image of this smart object
    private transient BufferedImage image = null;

    // It's important to call updateIconImage() only when we are
    // sure that the smart object's image is up-to-date, because otherwise
    // the filters could be started concurrently on different threads.
    private transient boolean iconImageNeedsRefresh = false;

    // constructor for converting a layer into a smart object
    public SmartObject(Layer layer) {
        super(layer.getComp(), NAME_PREFIX + layer.getName());

        Composition newContent = Composition.createEmpty(comp.getCanvasWidth(), comp.getCanvasHeight(), comp.getMode());
        // the mask stays outside the content, and will become the mask of the smart object
        Layer contentLayer = layer.copy(CopyType.UNDO, false, newContent);
        contentLayer.setName("original content", false);
        contentLayer.setHolder(newContent);
        newContent.addLayerInInitMode(contentLayer);
        newContent.setName(getName());
        newContent.createDebugName();
        setContent(newContent);

        copyLayerLevelPropertiesFrom(layer);

        assert checkInvariants();
    }

    // constructor for creating a smart object with a given content
    public SmartObject(Composition parent, Composition content) {
        super(parent, "Smart " + content.getName());
        setContent(content);

        assert checkInvariants();
    }

    // constructor called for "Add Linked"
    public SmartObject(File file, Composition parent, Composition content) {
        super(parent, file.getName());
        linkedContentFile = file;
        updateLinkedContentTime();
        setContent(content);

        assert checkInvariants();
    }

    // The constructor used for duplication.
    private SmartObject(SmartObject orig, CopyType copyType, Composition newComp) {
        super(orig.comp, copyType.createLayerCopyName(orig.getName()));
        assert orig.content.checkInvariants();
        if (copyType.doDeepContentCopy()) {
            Composition origContent = orig.content;
            Composition newContent = origContent.copy(copyType, true);
            setContent(newContent);
            assert origContent.checkInvariants();
        } else {
            setContent(orig.getContent());
        }
        image = orig.image;

        for (SmartFilter origFilter : orig.filters) {
            SmartFilter copy = (SmartFilter) origFilter.copy(copyType, true, newComp);
            copy.setSmartObject(this);
            addSmartFilter(copy, false, false);
        }

        linkedContentFile = orig.linkedContentFile;
        linkedContentFileTime = orig.linkedContentFileTime;
        if (orig.imageTransformer != null) {
            imageTransformer = orig.imageTransformer.copy(content);
            setBaseSource(imageTransformer);
        }
        setTranslation(orig.getTx(), orig.getTy());

        assert checkInvariants();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (!newVersion) {
            // if the pxc was saved with an old version,
            // then assume smart filter visibility
            smartFilterIsVisible = true;
        }

        // more initialization happens later in afterDeserialization()

//        System.out.printf("SmartObject::readObject: '%s' FINISHED on '%s'%n",
//            getName(), Thread.currentThread().getName());
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

    @Override
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
            filter.updateOptions();
        }

        recalculateImage();

        assert checkInvariants();
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
        if (contentTransform != null) {
            ImageTransformer transformer = new ImageTransformer(content, contentTransform, comp.getCanvasWidth(), comp.getCanvasHeight());
            setBaseSource(transformer);
            imageTransformer = transformer;
            contentTransform = null; // no longer needed
        } else {
            baseSource = content;
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
                    iconImageNeedsRefresh = true;
                    holder.update();
                }, onEDT);
        } else {
            // give up and use the previously created transparent image
            linkedContentFile = null;
        }
    }

    private void recalculateImage() {
        int numFilters = filters.size();
        if (numFilters > 0) {
            image = filters.get(numFilters - 1).getImage();
        } else {
            image = baseSource.getImage();
        }
        imageNeedsRefresh = false;
    }

    @Override
    public void update(Composition.UpdateActions actions) {
        if (imageNeedsRefresh) {
            recalculateImage();
        }
        holder.update(actions);
    }

    @Override
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

        // invalidate everything because if the content reference
        // didn't change, then setContent didn't do it
        iconImageNeedsRefresh = true;
        invalidateImageCache();
        invalidateAllFilterCaches();
        if (imageTransformer != null) {
            imageTransformer.invalidateCache();
        }

        holder.smartObjectChanged(isContentLinked());
    }

    private void invalidateAllFilterCaches() {
        if (!filters.isEmpty()) {
            filters.get(0).invalidateChain();
        }
    }

    public BufferedImage getVisibleImage() {
        if (imageNeedsRefresh) {
            recalculateImage();

            if (iconImageNeedsRefresh) {
                updateIconImage();
                iconImageNeedsRefresh = false;
            }
        }
        return image;
    }

    @Override
    protected String getRasterizedName() {
        return Utils.removePrefix(name, NAME_PREFIX);
    }

    @Override
    protected SmartObject createTypeSpecificCopy(CopyType copyType, Composition newComp) {
        return new SmartObject(this, copyType, newComp);
    }

    @Override
    public void replaceWithSmartObject() {
        String msg = format("<html>The layer <b>%s</b> is already a smart object.",
            getName());
        Messages.showInfo("Already a Smart Object", msg);
    }

    @Override
    protected void addSmartObjectMenus(JPopupMenu popup) {
        popup.add(new PAction("Edit Contents", this::edit));
        popup.add(new PAction("Clone", () ->
            comp.shallowDuplicate(this)));
        if (isContentLinked()) {
            popup.add(new PAction("Embed Contents", this::embedLinkedContent));
            popup.add(new PAction("Reload Contents", this::reloadLinkedContent));
        }
        if (SmartFilter.copiedSmartFilter != null) {
            popup.add(new PAction("Paste " + SmartFilter.copiedSmartFilter.getName(), () -> {
                // copy again, because it could be pasted multiple times
                SmartFilter newSF = (SmartFilter) SmartFilter.copiedSmartFilter.copy(CopyType.LAYER_DUPLICATE, true, comp);
                newSF.setSmartObject(this);
                addSmartFilter(newSF, true, true);
            }));
        }
        if (AppContext.isDevelopment()) {
            popup.add(new PAction("Debug Images", this::debugImages));
        }
    }

    @Override
    public boolean edit() {
        View contentView = content.getView();
        if (contentView == null) {
            Views.addAsNewComp(content);
            content.setDirty(false);
        } else {
            Views.activate(contentView);
        }
        return true;
    }

    public void forAllNestedSmartObjects(Consumer<SmartObject> action) {
        assert checkInvariants();

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

    public void tryAddingSmartFilter(Filter filter) {
        // adds a smart filter, and triggers an edit,
        // but if the user cancels the first edit,
        // then the filter is removed.
        SmartFilter smartFilter = new SmartFilter(filter, baseSource, this);

        if (filter instanceof FilterWithGUI) {
            smartFilter.setTentative(true);
            addSmartFilter(smartFilter, false, true);
            if (smartFilter.edit()) { // dialog accepted
                smartFilter.setTentative(false);
                History.add(new NewSmartFilterEdit(this, smartFilter));
            } else {
                deleteSmartFilter(smartFilter, false, true);
            }
        } else {
            addSmartFilter(smartFilter, true, true);
        }
    }

    public void addSmartFilter(SmartFilter newFilter, boolean addToHistory, boolean update) {
        int numFilters = filters.size();
        if (numFilters == 0) {
            // When duplicating a smart object, baseSource isn't
            // known yet. The first filter will be updated later.
            if (baseSource != null) {
                newFilter.setImageSource(baseSource);
            }
        }

        insertSmartFilter(newFilter, numFilters, update, update);

        if (addToHistory) {
            History.add(new NewSmartFilterEdit(this, newFilter));
        }
    }

    @Override
    public void insertLayer(Layer layer, int index, boolean update) {
        insertSmartFilter((SmartFilter) layer, index, update, true);
    }

    private void insertSmartFilter(SmartFilter newFilter, int index, boolean update, boolean activate) {
        assert newFilter.getHolder() == this;

        SmartFilter previous = null;
        if (index > 0) {
            previous = filters.get(index - 1);
        }

        int numFilters = filters.size();
        SmartFilter next = null;
        if (index < numFilters) {
            next = filters.get(index);
        }

        filters.add(index, newFilter);

        if (previous != null) {
            previous.setNext(newFilter);
            newFilter.setImageSource(previous);
        } else {
            newFilter.setImageSource(baseSource);
        }

        if (next != null) {
            newFilter.setNext(next);
            next.setImageSource(newFilter);
            next.invalidateChain();
        }

        if (update) {
            updateChildrenUI();
            if (activate) {
                comp.setActiveLayer(newFilter); // after creating the ui
            }

            invalidateImageCache();
            iconImageNeedsRefresh = true;
            holder.update();
            numFiltersChanged();
        } else if (activate) {
            comp.setActiveLayer(newFilter);
        }

        assert checkInvariants();
    }

    public void deleteSmartFilter(SmartFilter filter, boolean addToHistory, boolean update) {
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
                        next.setImageSource(baseSource);
                    }
                    comp.setActiveLayer(next);
                } else if (previous != null) {
                    comp.setActiveLayer(previous);
                } else {
                    comp.setActiveLayer(this);
                }
                filters.remove(i);
                break;
            }
        }

        if (index == -1) {
            throw new IllegalStateException(filter.getName() + " not found in " + getName());
        }

        filter.setNext(null);

        numFiltersChanged();
        if (addToHistory) {
            History.add(new DeleteLayerEdit(this, filter, index));
        }

        invalidateImageCache();
        iconImageNeedsRefresh = true;
        if (update) {
            holder.update();
            updateChildrenUI();
        }
        assert checkInvariants();
    }

    @Override
    public void deleteTemporarily(Layer layer) {
        deleteSmartFilter((SmartFilter) layer, false, false);
    }

    private void numFiltersChanged() {
        // notify the delete action only if not the whole
        // smart object is selected and not during construction
        if (!isActive() && hasUI()) {
            Layers.numLayersChanged(this, filters.size());
        }
    }

//    @Override
//    public BufferedImage getCanvasSizedSubImage() {
//        // workaround for moved layers
//        BufferedImage img = ImageUtils.createSysCompatibleImage(comp.getCanvas());
//        Graphics2D g = img.createGraphics();
//
//        // don't call applyLayer, because the mask should NOT be considered
//        setupDrawingComposite(g, true);
//        paintLayerOnGraphics(g, true);
//
//        g.dispose();
//        return img;
//    }

    public View getParentView() {
        if (isContentOpen()) {
            return content.getView();
        }
        return comp.getParentView();
    }

    public CompletableFuture<Composition> checkForAutoReload() {
        assert !isContentOpen();
        assert checkInvariants();

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

                // only a grandparent composition might be opened
                propagateContentChanges(loaded, true);
                getParentView().repaint();
                return loaded;
            }, onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    public Composition getContent() {
        return content;
    }

    public void setContent(Composition content) {
        if (this.content == content) {
            return;
        }
        this.content = content;
        content.addOwner(this);

        if (imageTransformer == null) {
            baseSource = content;
        } else {
            imageTransformer.setContent(content);
            baseSource = imageTransformer;
        }

        // if there are smart filters, the first one references the content
        if (filters != null) { // this check is required by the migration support
            if (!filters.isEmpty()) {
                SmartFilter first = filters.get(0);
                first.setImageSource(baseSource);
                first.invalidateChain();
            }
        }
        invalidateImageCache();

        assert checkInvariants();
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

    @Override
    public void setComp(Composition comp) {
        super.setComp(comp);

        for (SmartFilter filter : filters) {
            filter.setComp(comp);
        }
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

        if (imageTransformer == null) {
            imageTransformer = new ImageTransformer(content, newScaling, newSize.width, newSize.height);
            setBaseSource(imageTransformer);
        } else {
            imageTransformer.concatenate(newScaling, newSize.width, newSize.height);
            assert baseSource == imageTransformer;
        }
        invalidateAllFilterCaches();
        invalidateImageCache();

        if (filters.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (SmartFilter filter : filters) {
            if (filter.hasMask()) {
                futures.add(filter.getMask().resize(newSize));
            }
        }
        if (futures.isEmpty()) { // no smart filter has a mask
            return CompletableFuture.completedFuture(null);
        }

        return Utils.allOf(futures);
    }

    @Override
    public void flip(Flip.Direction direction) {
        AffineTransform flipTransform = direction.createImageTransform(image);
        int targetWidth = comp.getCanvasWidth();
        int targetHeight = comp.getCanvasHeight();
        if (imageTransformer == null) {
            imageTransformer = new ImageTransformer(content, flipTransform, targetWidth, targetHeight);
        } else {
            imageTransformer.concatenate(flipTransform, targetWidth, targetHeight);
        }
        invalidateAllFilterCaches();
        invalidateImageCache();

        for (SmartFilter filter : filters) {
            if (filter.hasMask()) {
                filter.getMask().flip(direction);
            }
        }
    }

    @Override
    public void rotate(QuadrantAngle angle) {
        AffineTransform rotation = angle.createImageTransform(image);
        if (contentTransform == null) {
            contentTransform = rotation;
        } else {
            contentTransform.concatenate(rotation);
        }
        invalidateAllFilterCaches();
        invalidateImageCache();
    }

    private void setBaseSource(ImageSource baseSource) {
        this.baseSource = baseSource;
        if (!filters.isEmpty()) {
            filters.get(0).setImageSource(baseSource);
        }
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int oldTx, int oldTy) {
        // a smart object never enlarges the image
        return new ContentLayerMoveEdit(this, null, oldTx, oldTy);
    }

//    @Override
//    public BufferedImage getCanvasSizedVisibleImage() {
//        int tx = getTx();
//        int ty = getTy();
//        BufferedImage visibleImage = getVisibleImage();
//        if (tx == 0 && ty == 0) {
//            assert visibleImage.getWidth() == comp.getCanvasWidth()
//                : "visible width = " + visibleImage.getWidth() + ", canvas width = " + comp.getCanvasWidth();
//            assert visibleImage.getHeight() == comp.getCanvasHeight()
//                : "visible height = " + visibleImage.getHeight() + ", canvas height = " + comp.getCanvasHeight();
//            return visibleImage;
//        }
//
//        // the image of a moved layer might not cover the canvas,
//        // therefore (unlike in the superclass) here we can't use subimage
//        BufferedImage img = ImageUtils.createSysCompatibleImage(comp.getCanvas());
//        Graphics2D g = img.createGraphics();
//        g.drawImage(visibleImage, tx, ty, null);
//        g.dispose();
//
//        return img;
//    }

    public SmartObject shallowDuplicate() {
        SmartObject d = new SmartObject(this, CopyType.SMART_OBJECT_CLONE, comp);
        duplicateMask(d, CopyType.SMART_OBJECT_CLONE, comp);
        return d;
    }

    public void moveUp(SmartFilter smartFilter) {
        int index = filters.indexOf(smartFilter);
        if (index == filters.size() - 1) {
            return; // already the last filter
        }
        swapSmartFilters(index, index + 1, "Move " + smartFilter.getName() + " Up");
        comp.setActiveLayer(smartFilter);
    }

    public void moveDown(SmartFilter smartFilter) {
        int index = filters.indexOf(smartFilter);
        if (index == 0) {
            return; // already the first filter
        }
        swapSmartFilters(index - 1, index, "Move " + smartFilter.getName() + " Down");
        comp.setActiveLayer(smartFilter);
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
        assert checkInvariants();

        if (editName != null) {
            History.add(new SwapSmartFiltersEdit(editName, this, indexA, indexB));
        }

        // update the GUI
        updateChildrenUI();

        // update the image
        SmartFilter lowestChanged = bellow != null ? bellow : filterB;
        lowestChanged.invalidateChain();
        invalidateImageCache();
        holder.update();

        Layers.layerOrderChanged(this);
    }

    public boolean containsSmartFilter(SmartFilter filter) {
        return filters.contains(filter);
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
            if (filter.isActive()) {
                return filter;
            }
        }
        return null;
    }

    @Override
    public void forEachNestedLayer(Consumer<Layer> action, boolean includeMasks) {
        action.accept(this);
        if (includeMasks && hasMask()) {
            action.accept(getMask());
        }
        for (SmartFilter filter : filters) {
            action.accept(filter);
            if (includeMasks && filter.hasMask()) {
                action.accept(filter.getMask());
            }
        }
    }

    @Override
    public boolean checkInvariants() {
        if (!super.checkInvariants()) {
            return false;
        }

        if (!content.isSmartObjectContent()) {
            throw new IllegalStateException("content of %s (%s) is broken"
                .formatted(getName(), content.getDebugName()));
        }
        if (!content.getOwners().contains(this)) {
            throw new AssertionError(getName() + " not owner of its content");
        }
        if (baseSource == null) {
            throw new AssertionError("no base source");
        }
        if (filters != null) { // pxc might not be migrated yet
            for (int i = 0; i < filters.size(); i++) {
                SmartFilter filter = filters.get(i);
                if (!filter.checkInvariants()) {
                    return false;
                }
                if (i == 0) {
                    if (filter.getImageSource() != baseSource) {
                        throw new AssertionError("first filter (%s) doesn't use baseSource".formatted(filter.getName()));
                    }
                } else {
                    if (filter.getImageSource() != filters.get(i - 1)) {
                        throw new AssertionError("bad source in " + filter.getName());
                    }
                }
                if (i == filters.size() - 1) {
                    if (filter.getNext() != null) {
                        throw new AssertionError("last filter has next");
                    }
                } else {
                    if (filter.getNext() != filters.get(i + 1)) {
                        throw new AssertionError("bad next in " + filter.getName());
                    }
                }
            }
        }
        if (imageTransformer != null) {
            if (imageTransformer != baseSource) {
                throw new AssertionError();
            }
            if (!filters.isEmpty()) {
                if (filters.get(0).getImageSource() != imageTransformer) {
                    throw new AssertionError();
                }
            }
        }
        if (contentTransform != null) {
            // old field, used only for migration
            throw new AssertionError();
        }
        if (!content.checkInvariants()) {
            return false;
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

    public void debugImages() {
        BufferedImage soImage = image;
        BufferedImage contentImage = content.getCompositeImage();
//        BufferedImage calcContentImage = content.calculateCompositeImage();

        Debug.debugImage(soImage, "image");
        Debug.debugImage(contentImage, "contentImage");
        if (imageTransformer != null) {
            BufferedImage transformerCache = imageTransformer.getCachedImage();
            if (transformerCache != null) {
                Debug.debugImage(transformerCache, "transformerCache");
            }
        }
        for (SmartFilter filter : filters) {
            BufferedImage filterCache = filter.getCachedImage();
            if (filterCache != null) {
                Debug.debugImage(filterCache, "filterCache for " + filter.getName());
            }
        }
//        Debug.debugImage(calcContentImage, "calcContentImage");
    }

    public int indexOf(SmartFilter sf) {
        return filters.indexOf(sf);
    }

    @Override
    public int getActiveLayerIndex() {
        SmartFilter sf = getSelectedSmartFilter();
        if (sf != null) {
            return filters.indexOf(sf);
        }
        return -1; // TODO
    }

    @Override
    public int indexOf(Layer layer) {
        return filters.indexOf((SmartFilter) layer);
    }

    @Override
    public int getNumLayers() {
        return filters.size();
    }

    @Override
    public SmartFilter getLayer(int index) {
        return filters.get(index);
    }

    @Override
    public Stream<? extends Layer> levelStream() {
        return filters.stream();
    }

    @Override
    public void addLayerToList(int index, Layer newLayer) {
        // TODO is this ever called?
        filters.add(index, (SmartFilter) newLayer);
    }

    @Override
    public void moveActiveLayerUp() {
        SmartFilter sf = getSelectedSmartFilter();
        if (sf != null) {
            moveUp(sf);
        }
    }

    @Override
    public void moveActiveLayerDown() {
        SmartFilter sf = getSelectedSmartFilter();
        if (sf != null) {
            moveDown(sf);
        }
    }

    @Override
    public void deleteLayer(Layer layer, boolean addToHistory) {
        deleteSmartFilter((SmartFilter) layer, addToHistory, true);
    }

    @Override
    public boolean allowZeroLayers() {
        return true;
    }

    @Override
    public boolean contains(Layer layer) {
        if (layer == this) {
            return true;
        }
        for (SmartFilter filter : filters) {
            if (filter == layer) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BufferedImage applyOnImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
        BufferedImage visibleImage = getVisibleImage();
        paintLayerOnGraphicsWOTmpLayer(g, visibleImage, firstVisibleLayer);
    }

    protected void paintLayerOnGraphicsWOTmpLayer(Graphics2D g,
                                                  BufferedImage visibleImage,
                                                  boolean firstVisibleLayer) {
        g.drawImage(visibleImage, getTx(), getTy(), null);
    }

    /**
     * Returns the image bounds relative to the canvas
     */
    @Override
    public Rectangle getContentBounds() {
        return new Rectangle(getTx(), getTy(), image.getWidth(), image.getHeight());
    }

    @Override
    public int getPixelAtPoint(Point p) {
        return 0; // TODO
    }

    @Override
    public void enlargeCanvas(int north, int east, int south, int west) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceLayer(Layer before, Layer after) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changeLayerGUIOrder(int oldIndex, int newIndex) {
        updateChildrenUI();
    }

    @Override
    public void removeLayerFromList(Layer layer) {
        // it's not enough to remove it from the list,
        // invariants have to be preserved
        deleteSmartFilter((SmartFilter) layer, false, false);
    }

    @Override
    public BufferedImage createIconThumbnail() {
//        BufferedImage bigImg = getCanvasSizedSubImage();
        // TODO is the image always canvas-sized?
        return createThumbnail(getVisibleImage(), thumbSize, thumbCheckerBoardPainter);
    }

    @Override
    public void smartObjectChanged(boolean linked) {
        throw new IllegalStateException();
    }

    @Override
    public String getORAStackXML() {
        throw new IllegalStateException();
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
        if (imageTransformer != null) {
            node.add(imageTransformer.createDebugNode("imageTransformer"));
        }

        return node;
    }
}
