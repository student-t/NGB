import {
    AminoacidFeatureRenderer,
    FeatureBaseRenderer,
    GeneFeatureRenderer,
    TranscriptFeatureRenderer,
    ZONES_MANAGER_DEFAULT_ZONE_NAME
} from './drawing';
import {Sorting, ZonesManager} from '../../../../../utilities';
import PIXI from 'pixi.js';
import {Viewport} from '../../../../../core';

const Math = window.Math;

export default class FeatureRenderer {

    _config;
    _zonesManager: ZonesManager = new ZonesManager();

    _geneFeatureRenderer: GeneFeatureRenderer = null;
    _transcriptFeatureRenderer: TranscriptFeatureRenderer = null;
    _aminoacidFeatureRenderer: AminoacidFeatureRenderer = null;

    _labels = null;
    _dockableElements = null;

    _featuresPositions = null;
    _opts;

    constructor(config) {
        this._config = config;
        this._aminoacidFeatureRenderer = new AminoacidFeatureRenderer(config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition);
        this._transcriptFeatureRenderer = new TranscriptFeatureRenderer(config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            this._aminoacidFeatureRenderer);
        this._geneFeatureRenderer = new GeneFeatureRenderer(config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            this._transcriptFeatureRenderer);

        this._labels = [];
        this._dockableElements = [];
    }

    get defaultRenderer(): FeatureBaseRenderer {
        return this._geneFeatureRenderer;
    }

    get textureCoordinates() {
        const coordinates = {
            x: null,
            y: null
        };
        const updateCoordinates = function(newCoordinates) {
            if (!newCoordinates) {
                return;
            }
            const {x, y} = newCoordinates;
            if (x !== null && (!coordinates.x || coordinates.x > x)) {
                coordinates.x = x;
            }
            if (y !== null && (!coordinates.y || coordinates.y > y)) {
                coordinates.y = y;
            }
        };
        updateCoordinates(this._geneFeatureRenderer.textureCoordinates);
        updateCoordinates(this._transcriptFeatureRenderer.textureCoordinates);
        updateCoordinates(this._aminoacidFeatureRenderer.textureCoordinates);
        return coordinates;
    }

    prepare() {
        this._zonesManager.configureZone(ZONES_MANAGER_DEFAULT_ZONE_NAME, {y1: this._config.levels.margin});
    }

    getActualHeight() {
        return this._zonesManager.getZoneActualRange(ZONES_MANAGER_DEFAULT_ZONE_NAME).y2;
    }

    prepareRenderers() {
        this._geneFeatureRenderer.initializeRenderingSession();
        this._transcriptFeatureRenderer.initializeRenderingSession();
        this._aminoacidFeatureRenderer.initializeRenderingSession();
    }

    render(features,
           viewport: Viewport,
           labelContainer: PIXI.Container,
           dockableElementsContainer: PIXI.Container,
           graphics: PIXI.Graphics = null): PIXI.Graphics {
        if (features === null || features === undefined)
            return null;
        this.prepareRenderers();
        this._labels = [];
        this._dockableElements = [];
        this._featuresPositions = [];
        const zoneBoundaries = this._zonesManager.getZoneBoundaries(ZONES_MANAGER_DEFAULT_ZONE_NAME);
        if (zoneBoundaries === null)
            return null;
        let featureGraphics = graphics;
        if (featureGraphics === null || featureGraphics === undefined) {
            featureGraphics = new PIXI.Graphics();
        }
        this._geneFeatureRenderer._opts = this._opts;
        const maxIterations = 20;
        for (let i = 0; i < features.length; i++) {
            const item = features[i];
            const boundaries = this._zonesManager.checkArea(
                ZONES_MANAGER_DEFAULT_ZONE_NAME,
                Object.assign(
                    {
                        global: {
                            x: 0,
                            y: zoneBoundaries.y1
                        }
                    },
                    this.defaultRenderer.analyzeBoundaries(item, viewport)
                ),
                {
                    translateX: 0,
                    translateY: 1
                },
                maxIterations);
            if (!boundaries.conflicts) {
                this._zonesManager.submitArea(ZONES_MANAGER_DEFAULT_ZONE_NAME, boundaries);
                this.defaultRenderer.render(item, viewport, featureGraphics, labelContainer, dockableElementsContainer, {
                    height: boundaries.rect.y2 - boundaries.rect.y1,
                    width: boundaries.rect.x2 - boundaries.rect.x1,
                    x: boundaries.rect.x1,
                    y: boundaries.rect.y1 + zoneBoundaries.y1
                });
            }
        }
        return featureGraphics;
    }

    manageLabels(viewport) {
        for (let i = 0; i < this._labels.length; i++) {
            const labelData = this._labels[i];
            if (labelData.label.parent === null || labelData.label.parent === undefined)
                continue;
            if (labelData.centered) {
                labelData.label.x = Math.round(viewport.project.brushBP2pixel(labelData.range.start) +
                    viewport.convert.brushBP2pixel(labelData.range.end - labelData.range.start) / 2 -
                    labelData.label.width / 2);
            }
            else {
                const startPx = viewport.project.brushBP2pixel(labelData.range.start) +
                    (labelData.range.offset ? labelData.range.offset.left : 0);
                const endPx = Math.max(viewport.project.brushBP2pixel(labelData.range.end) -
                    labelData.label.width, startPx);
                if (startPx > viewport.canvasSize || endPx < -labelData.label.width) {
                    labelData.label.visible = false;
                }
                else {
                    labelData.label.visible = true;
                    labelData.label.x = Math.round(Math.max(startPx, Math.min(endPx, 0))) - labelData.label.parent.x;
                }

                if (labelData.yDockable && labelData.range.height !== undefined) {
                    const relativeYStartPosition = labelData.label.parent.y + labelData.position.y;
                    const relativeYEndPosition = labelData.label.parent.y + labelData.position.y + labelData.range.height - labelData.label.height;
                    labelData.label.y = Math.round(Math.max(relativeYStartPosition, Math.min(relativeYEndPosition, 0))) - labelData.label.parent.y;
                }
            }
        }
    }

    manageDockableElements() {
        for (let i = 0; i < this._dockableElements.length; i++) {
            const dockableElementData = this._dockableElements[i];
            if (dockableElementData.element.parent === null || dockableElementData.element.parent === undefined)
                continue;
            const relativeYStartPosition = dockableElementData.element.parent.y + dockableElementData.range.y1;
            const relativeYEndPosition = dockableElementData.element.parent.y + dockableElementData.range.y2;
            dockableElementData.element.y = Math.round(Math.max(relativeYStartPosition,
                    Math.min(relativeYEndPosition, dockableElementData.range.topMargin))) -
                dockableElementData.element.parent.y;
        }
    }

    _getDockableElementsBoundaries(viewport) {
        const yBorder = 50; // we should manage only 'upper' graphics
        const rects = [];
        for (let i = 0; i < this._dockableElements.length; i++) {
            const element = this._dockableElements[i].element;
            if (element.parent === null || element.parent === undefined) {
                continue;
            }
            const relativeYStartPosition = element.parent.y + this._dockableElements[i].range.y1;
            if (relativeYStartPosition > this._dockableElements[i].range.topMargin)
                continue;
            const x1 = element.x + element.parent.x - 1;
            const x2 = x1 + element.width + 1;
            const y1 = element.y + element.parent.y;
            const y2 = y1 + 1;
            if (x2 < 0 || x1 > viewport.canvasSize || y1 < 0 || y2 > yBorder)
                continue;
            rects.push({
                x1: x1,
                x2: x2,
                y1: 0,
                y2: y2
            });
        }
        return rects;
    }

    _getLabelsBoundaries(viewport) {
        const yBorder = 50; // we should manage only 'upper' graphics
        const rects = [];
        for (let i = 0; i < this._labels.length; i++) {
            if (!this._labels[i].yDockable)
                continue;
            const element:PIXI.Text = this._labels[i].label;
            if (element.parent === null || element.parent === undefined) {
                continue;
            }
            const x1 = element.x + element.parent.x;
            const x2 = x1 + element.width;
            const y1 = element.y + element.parent.y;
            const y2 = y1 + element.height;
            if (x2 < 0 || x1 > viewport.canvasSize || y1 < 0 || y2 > yBorder)
                continue;
            rects.push({
                x1: x1,
                x2: x2,
                y1: y1,
                y2: y2
            });
        }
        return rects;
    }

    manageMask(mask, viewport) {
        mask.clear();
        const rects = [];
        rects.push(...this._getDockableElementsBoundaries(viewport));
        rects.push(...this._getLabelsBoundaries(viewport));
        let xCoords = [];
        let yCoords = [];
        xCoords.push(-viewport.canvasSize);
        xCoords.push(2 * viewport.canvasSize);
        yCoords.push(0);
        yCoords.push(this.getActualHeight());
        for (let i = 0; i < rects.length; i++) {
            if (xCoords.indexOf(rects[i].x1) === -1)
                xCoords.push(rects[i].x1);
            if (xCoords.indexOf(rects[i].x2) === -1)
                xCoords.push(rects[i].x2);
            if (yCoords.indexOf(rects[i].y1) === -1)
                yCoords.push(rects[i].y1);
            if (yCoords.indexOf(rects[i].y2) === -1)
                yCoords.push(rects[i].y2);
        }
        xCoords = Sorting.quickSort(xCoords, true);
        yCoords = Sorting.quickSort(yCoords, true);
        const rectIsEmpty = (rect, rects) => {
            for (let i = 0; i < rects.length; i++) {
                if (!(rect.x2 <= rects[i].x1 || rects[i].x2 <= rect.x1 || rect.y2 <= rects[i].y1 || rects[i].y2 <= rect.y1))
                    return false;
            }
            return true;
        };
        const maskFill = 0xFFFFFF;
        mask.beginFill(maskFill, 1);
        for (let i = 0; i < xCoords.length - 1; i++) {
            for (let j = 0; j < yCoords.length - 1; j++) {
                const rect = {
                    x1: xCoords[i],
                    x2: xCoords[i + 1],
                    y1: yCoords[j],
                    y2: yCoords[j + 1]
                };
                if (rectIsEmpty(rect, rects)) {
                    mask.drawRect(rect.x1, rect.y1, rect.x2 - rect.x1, rect.y2 - rect.y1);
                }
            }
        }
        mask.endFill();
    }

    registerLabel(label, position, range, yDockable = false, centered = false) {
        this._labels.push({
            centered: centered,
            label: label,
            position: position,
            range: range,
            yDockable
        });
    }

    registerDockableElement(dockableElement, range) {
        this._dockableElements.push({
            element: dockableElement,
            range: range
        });
    }

    registerFeaturePosition(feature, boundaries) {
        this._featuresPositions.push({
            boundaries,
            feature
        });
    }

    checkPosition(position, relativeContainer) {
        const result = [];
        if (this._featuresPositions === null)
            return result;
        for (let i = 0; i < this._featuresPositions.length; i++) {
            if (position.x - relativeContainer.x >= this._featuresPositions[i].boundaries.x1 &&
                position.x - relativeContainer.x <= this._featuresPositions[i].boundaries.x2 &&
                position.y - relativeContainer.y >= this._featuresPositions[i].boundaries.y1 &&
                position.y - relativeContainer.y <= this._featuresPositions[i].boundaries.y2) {
                const featureName = this._featuresPositions[i].feature.feature ?
                    this._featuresPositions[i].feature.feature.toLowerCase() : null;
                result.push(this._featuresPositions[i].feature);
                if (featureName && (featureName === 'mrna' || featureName === 'transcript')) {
                    break;
                }
            }
        }
        return result;
    }
}
