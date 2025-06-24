/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.AppMode;
import pixelitor.Composition;
import pixelitor.CopyType;
import pixelitor.Views;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.gui.View;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.TaskAction;
import pixelitor.history.*;
import pixelitor.io.FileChoosers;
import pixelitor.io.FileIO;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
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
import java.util.stream.Stream;

import static pixelitor.Views.thumbSize;
import static pixelitor.utils.ImageUtils.createThumbnail;
import static pixelitor.utils.Threads.onEDT;

/**
 * A layer that embeds a {@link Composition},
 * and supports non-destructive editing via smart filters.
 */
public class SmartObject extends CompositeLayer {
    @Serial
    private static final long serialVersionUID = 8594248957749192719L;

    private static final String NAME_PREFIX = "smart ";
    private Composition content;

    // Only used for a deserialization check.
    // The field is not present in very old pxc versions.
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
    private boolean newVersion = true;

    // The file containing the linked content, if this smart object's content is linked.
    // This is the same object as the content's file field, but it's not transient.
    private File linkedContentFile;

    // Timestamp of the last known modification to the linked content file.
    private transient long linkedContentTimestamp;

    // "Legacy" fields from Pixelitor 4.3.0, they are only
    // used for automatic migration of old pxc files
    private boolean smartFilterIsVisible = true;
    private List<Filter> smartFilters = new ArrayList<>();

    // the real list of smart filters
    private List<SmartFilter> filters = new ArrayList<>();

    // the source of the starting image for the smart filters:
    // either the content or the transformer, if there is one.
    private ImageSource baseSource;

    // transformer in old pxc files
    private AffineTransform contentTransform;

    // transformer in new pxc files
    private ImageTransformer imageTransformer;

    // the cached image of this smart object
    private transient BufferedImage image = null;

    private transient boolean imageNeedsRefresh = false;

    // It's important to call updateIconImage() only when we are
    // sure that the smart object's image is up-to-date, because otherwise
    // the filters could be started concurrently on different threads.
    private transient boolean iconImageNeedsRefresh = false;

    // constructor for converting a layer into a smart object
    public SmartObject(Layer layer) {
        super(layer.getComp(), NAME_PREFIX + layer.getName());

        createContentFrom(layer);

        layer.copyCommonPropertiesTo(this);

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
        updateLinkedContentTimestamp();
        setContent(content);

        assert checkInvariants();
    }

    // The constructor used for duplication.
    private SmartObject(SmartObject orig, CopyType copyType, Composition newComp) {
        super(orig.comp, copyType.createLayerCopyName(orig.getName()));
        assert orig.content.checkInvariants();
        if (copyType.isDeepContentCopy()) {
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
        linkedContentTimestamp = orig.linkedContentTimestamp;
        if (orig.imageTransformer != null) {
            imageTransformer = orig.imageTransformer.copy(content);
            setBaseImageSource(imageTransformer);
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

        this.imageNeedsRefresh = false;
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

    @Override
    public void afterDeserialization() {
        if (isContentLinked()) {
            if (linkedContentFile.exists()) {
                updateLinkedContentTimestamp();
                // also read the content
                assert content == null;
                setContent(FileIO.loadCompSync(linkedContentFile));
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
            filter.adaptToContext();
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
            setBaseImageSource(transformer);
            imageTransformer = transformer;
            contentTransform = null; // no longer needed
        } else {
            if (imageTransformer != null) {
                assert baseSource == imageTransformer;
                baseSource = imageTransformer;
            } else {
                baseSource = content;
            }
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
            updateLinkedContentTimestamp();

            FileIO.loadCompAsync(linkedContentFile)
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

    private void createContentFrom(Layer layer) {
        Composition newContent = Composition.createEmpty(comp.getCanvasWidth(), comp.getCanvasHeight(), comp.getMode());
        // the mask stays outside the content, and will become the mask of the smart object
        if (layer instanceof LayerGroup group) {
            // flatten the contents of the group
            int numLayers = group.getNumLayers();
            for (int i = 0; i < numLayers; i++) {
                Layer layerCopy = group.getLayer(i).copy(CopyType.UNDO, true, newContent);
                newContent.addLayerWithoutUI(layerCopy);
            }
        } else {
            Layer contentLayer = layer.copy(CopyType.UNDO, false, newContent);
            contentLayer.setName("original content", false);
            contentLayer.setHolder(newContent);
            newContent.addLayerWithoutUI(contentLayer);
        }
        newContent.setName(getName());
        newContent.createDebugName();
        setContent(newContent);
    }

    private void recalculateImage() {
        image = filters.isEmpty()
            ? baseSource.getImage()
            : filters.getLast().getImage();
        imageNeedsRefresh = false;
    }

    @Override
    public void update(boolean updateHistogram) {
        if (imageNeedsRefresh) {
            recalculateImage();
        }
        holder.update(updateHistogram);
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
        invalidateAllCaches(true);

        holder.smartObjectChanged(isContentLinked());
    }

    private void invalidateAllCaches(boolean invalidateTransformer) {
        iconImageNeedsRefresh = true;
        invalidateImageCache();
        invalidateFilterCaches();

        if (invalidateTransformer && imageTransformer != null) {
            imageTransformer.invalidateCache();
        }
    }

    private void invalidateFilterCaches() {
        if (!filters.isEmpty()) {
            filters.getFirst().invalidateChain();
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
        throw new IllegalStateException(); // it's already smart
    }

    @Override
    public boolean isConvertibleToSmartObject() {
        return false;
    }

    @Override
    protected void addSmartObjectMenus(JPopupMenu popup) {
        popup.add(new TaskAction("Edit Contents", this::edit));
        popup.add(new TaskAction("Clone", () ->
            comp.shallowDuplicate(this)));
        if (isContentLinked()) {
            popup.add(new TaskAction("Embed Contents", this::embedLinkedContent));
            popup.add(new TaskAction("Reload Contents", this::reloadLinkedContentAsync));
        }
        if (SmartFilter.copiedSmartFilter != null) {
            popup.add(new TaskAction("Paste " + SmartFilter.copiedSmartFilter.getName(), () -> {
                // copy again, because it could be pasted multiple times
                SmartFilter newSF = (SmartFilter) SmartFilter.copiedSmartFilter.copy(CopyType.DUPLICATE_LAYER, true, comp);
                newSF.setSmartObject(this);
                addSmartFilter(newSF, true, true);
            }));
        }
        if (AppMode.isDevelopment()) {
            popup.add(new TaskAction("Debug Images", this::debugImages));
        }
    }

    @Override
    public boolean edit() {
        View contentView = content.getView();
        if (contentView == null) {
            Views.addNew(content);
            content.setDirty(false);
        } else { // the view is already opened
            Views.activate(contentView);
        }
        return true;
    }

    public void forEachNestedSmartObject(Consumer<SmartObject> action) {
        assert checkInvariants();

        action.accept(this);
        content.forEachNestedSmartObject(action);
    }

    public int getNumSmartFilters() {
        return filters.size();
    }

    public SmartFilter getSmartFilter(int index) {
        return filters.get(index);
    }

    // adds a smart filter, and shows its configuration dialog,
    // but if the user cancels the dialog, the filter is removed
    public void tryAddingSmartFilter(Filter filter) {
        SmartFilter smartFilter = new SmartFilter(filter, baseSource, this);
        boolean hasDialog = filter instanceof FilterWithGUI;
        addSmartFilter(smartFilter, !hasDialog, true);

        if (hasDialog) {
            smartFilter.setTentative(true);
            if (smartFilter.edit()) { // dialog accepted
                smartFilter.setTentative(false);
                History.add(new NewSmartFilterEdit(this, smartFilter));
            } else {
                deleteSmartFilter(smartFilter, false, true);
            }
        }
    }

    public void addSmartFilter(SmartFilter newFilter, boolean addToHistory, boolean update) {
        insertSmartFilter(newFilter, filters.size(), update, update);

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

        SmartFilter previous = (index == 0) ? null : filters.get(index - 1);
        SmartFilter next = (index == filters.size()) ? null : filters.get(index);

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
        int index = filters.indexOf(filter);
        if (index == -1) {
            throw new IllegalStateException(filter.getName() + " not found in " + getName());
        }
        deleteSmartFilterAtIndex(index);

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

    private void deleteSmartFilterAtIndex(int i) {
        SmartFilter previous = (i > 0) ? filters.get(i - 1) : null;
        SmartFilter next = (i < filters.size() - 1) ? filters.get(i + 1) : null;

        if (previous != null) {
            previous.setNext(next);
        }
        if (next != null) {
            next.invalidateChain();
            next.setImageSource(previous != null ? previous : baseSource);
            comp.setActiveLayer(next);
        } else if (previous != null) {
            comp.setActiveLayer(previous);
        } else {
            comp.setActiveLayer(this);
        }
        filters.remove(i);
    }

    @Override
    public void deleteInternal(Layer layer) {
        deleteSmartFilter((SmartFilter) layer, false, false);
    }

    private void numFiltersChanged() {
        // notify the delete action only if not the whole
        // smart object is selected and not during construction
        if (!isActive() && hasUI()) {
            Layers.numLayersChanged(this, filters.size());
        }
    }

    public View findParentView() {
        if (isContentOpen()) {
            return content.getView();
        }
        return comp.findParentView();
    }

    public CompletableFuture<Composition> checkForAutoReload() {
        assert !isContentOpen();
        assert checkInvariants();

        if (isContentLinked()) {
            long currentTimestamp = linkedContentFile.lastModified();
            if (currentTimestamp > linkedContentTimestamp) {
                linkedContentTimestamp = currentTimestamp;

                Views.activate(findParentView());
                boolean shouldReload = Messages.showReloadFileQuestion(linkedContentFile);
                if (shouldReload) {
                    // if this content is reloaded, then return because
                    // the nested smart objects don't have to be checked
                    return reloadLinkedContentAsync();
                }
            }
        }

        // recursively check nested smart objects within the content
        return content.checkForExternalModifications();
    }

    private CompletableFuture<Composition> reloadLinkedContentAsync() {
        return reloadContent(linkedContentFile);
    }

    private CompletableFuture<Composition> reloadContent(File file) {
        return FileIO.loadCompAsync(file)
            .thenApplyAsync(loadedContent -> {
                setContent(loadedContent);
                propagateContentChanges(loadedContent, true);
                findParentView().repaint();
                return loadedContent;
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
                SmartFilter first = filters.getFirst();
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

    public boolean isContentEmbedded() {
        return !isContentLinked();
    }

    public void setLinkedContentFile(File file) {
        this.linkedContentFile = file;
        updateLinkedContentTimestamp();
    }

    private void updateLinkedContentTimestamp() {
        linkedContentTimestamp = linkedContentFile.lastModified();
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
     * Returns the composition responsible for saving the contents of this smart object to its file.
     */
    public Composition findSavingComp() {
        SmartObject currentSO = this;

        // traverses up through potentially multiple levels of nesting
        // until it finds either a linked content or a top-level composition
        while (true) {
            // if the content is linked (saved as separate file),
            // then the content is responsible for saving itself
            if (currentSO.isContentLinked()) {
                return currentSO.getContent();
            }

            Composition parent = currentSO.getComp();
            if (parent.isSmartObjectContent()) {
                // we assume all smart objects having this content are
                // in the same composition, so we can take any one of
                // them to continue traversing up the hierarchy
                currentSO = parent.getOwners().getFirst();
            } else {
                // we've reached a top-level composition (not smart
                // object content), which saves all its embedded content
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
            setBaseImageSource(imageTransformer);
        } else {
            imageTransformer.concatenate(newScaling, newSize.width, newSize.height);
            assert baseSource == imageTransformer;
        }
        invalidateAllCaches(false);

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
    public void flip(FlipDirection direction) {
        AffineTransform flipTransform = direction.createImageTransform(image);
        int targetWidth = comp.getCanvasWidth();
        int targetHeight = comp.getCanvasHeight();
        if (imageTransformer == null) {
            imageTransformer = new ImageTransformer(content, flipTransform, targetWidth, targetHeight);
        } else {
            imageTransformer.concatenate(flipTransform, targetWidth, targetHeight);
        }
        invalidateAllCaches(false);

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
        invalidateAllCaches(false);
    }

    private void setBaseImageSource(ImageSource baseSource) {
        this.baseSource = baseSource;
        if (!filters.isEmpty()) {
            filters.getFirst().setImageSource(baseSource);
        }
    }

    @Override
    ContentLayerMoveEdit createMovementEdit(int prevTx, int prevTy) {
        // a smart object never enlarges the image
        return new ContentLayerMoveEdit(this, null, prevTx, prevTy);
    }

    public SmartObject shallowDuplicate() {
        SmartObject d = new SmartObject(this, CopyType.CLONE_SMART_OBJECT, comp);
        copyMaskTo(d, CopyType.CLONE_SMART_OBJECT, comp);
        return d;
    }

    @Override
    public void reorderActiveLayer(boolean up) {
        SmartFilter smartFilter = getSelectedSmartFilter();
        if (smartFilter != null) {
            move(smartFilter, up);
        }
    }

    public void moveUp(SmartFilter smartFilter) {
        move(smartFilter, true);
    }

    public void moveDown(SmartFilter smartFilter) {
        move(smartFilter, false);
    }

    public void move(SmartFilter smartFilter, boolean up) {
        int index = filters.indexOf(smartFilter);
        if ((up && index == filters.size() - 1) || (!up && index == 0)) {
            return; // already at the boundary
        }
        String editName = "Move " + smartFilter.getName() + (up ? " Up" : " Down");
        int indexA = up ? index : index - 1;
        int indexB = indexA + 1;
        swapSmartFilters(indexA, indexB, editName);
        comp.setActiveLayer(smartFilter);
    }

    public void swapSmartFilters(int indexA, int indexB, String editName) {
        assert indexB == indexA + 1;

        SmartFilter filterA = filters.get(indexA);
        SmartFilter filterB = filters.get(indexB);
        assert filterA.getNext() == filterB;
        assert filterB.getImageSource() == filterA;

        SmartFilter below = (indexA == 0) ? null : filters.get(indexA - 1);
        SmartFilter above = filterB.getNext();
        ImageSource origSource = filterA.getImageSource();

        Collections.swap(filters, indexA, indexB);

        if (below != null) {
            assert below.getNext() == filterA;
            below.setNext(filterB);
        }

        if (above != null) {
            filterA.setNext(above);
            above.setImageSource(filterA);
        } else {
            filterA.setNext(null);
        }

        filterB.setNext(filterA);
        filterA.setImageSource(filterB);
        filterB.setImageSource(origSource);

        assert checkInvariants();

        if (editName != null) {
            History.add(new SwapSmartFiltersEdit(editName, this, indexA, indexB));
        }

        // update the GUI
        updateChildrenUI();

        // update the image
        SmartFilter lowestChanged = below != null ? below : filterB;
        lowestChanged.invalidateChain();
        invalidateImageCache();
        holder.update();

        Layers.layersReordered(this);
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
        } else {
            // edit the last smart filter
            filters.getLast().edit();
        }
    }

    private SmartFilter getSelectedSmartFilter() {
        for (SmartFilter filter : filters) {
            if (filter.isActive()) {
                return filter;
            }
        }
        return null; // the smart object as a whole is selected
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
                if (filters.getFirst().getImageSource() != imageTransformer) {
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

    private void debugImages() {
        BufferedImage soImage = image;
        BufferedImage contentImage = content.getCompositeImage();

        Debug.debugImage(soImage, "image");
        Debug.debugImage(contentImage, "contentImage");
        if (imageTransformer != null) {
            BufferedImage transformerCache = imageTransformer.getCachedImage();
            if (transformerCache != null) {
                Debug.debugImage(transformerCache, "transformerCache");
            }
        }
        for (SmartFilter filter : filters) {
            BufferedImage filterCache = filter.getOutputCache();
            if (filterCache != null) {
                Debug.debugImage(filterCache, "filterCache for " + filter.getName());
            }
        }
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
        return -1;
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
    public boolean listContainsLayer(Layer layer) {
        return filters.contains((SmartFilter) layer);
    }

    @Override
    public Stream<? extends Layer> levelStream() {
        return filters.stream();
    }

    @Override
    public void addLayerToList(Layer newLayer, int index) {
        SmartFilter newSmartFilter = (SmartFilter) newLayer;

        // This code is called when duplicating a smart filter.
        newSmartFilter.invalidateCache();

        insertSmartFilter(newSmartFilter, index, false, false);

        // Update and setActiveLayer will be called later, but this is necessary.
        invalidateImageCache();
        iconImageNeedsRefresh = true;
    }

    @Override
    public void deleteLayer(Layer layer, boolean addToHistory) {
        deleteSmartFilter((SmartFilter) layer, addToHistory, true);
    }

    @Override
    public boolean canBeEmpty() {
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
    public boolean containsLayerOfType(Class<? extends Layer> type) {
        // Check if the smart object itself is of the given type
        if (type.isInstance(this)) {
            return true;
        }

        // check if any of the smart filters are of the given type
        for (SmartFilter filter : filters) {
            if (filter.containsLayerOfType(type)) {
                return true;
            }
        }

        // stop here: don't check the content, because it's not needed by the callers
        return false;
    }

    @Override
    protected BufferedImage transformImage(BufferedImage src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void paint(Graphics2D g, boolean firstVisibleLayer) {
        g.drawImage(getVisibleImage(), getTx(), getTy(), null);
    }

    /**
     * Returns the image bounds relative to the canvas
     */
    @Override
    public Rectangle getContentBounds(boolean includeTransparent) {
        return new Rectangle(getTx(), getTy(), image.getWidth(), image.getHeight());
    }

    @Override
    public int getPixelAtPoint(Point p) {
        if (image != null) {
            return ImageUtils.getPixelAt(this, image, p);
        }
        return 0;
    }

    @Override
    public void enlargeCanvas(Outsets out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceLayer(Layer before, Layer after) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reorderLayerUI(int oldIndex, int newIndex) {
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
        // TODO the thumbnail currently isn't created from the
        //   canvas-visible region of the image
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
