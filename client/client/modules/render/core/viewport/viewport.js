import BaseViewport from './baseViewport';
import KeyboardJS from 'keyboardjs';
import {ShortenedIntronsViewport} from './shortenedIntrons';
import {Subject} from 'rx';
import angular from 'angular';

//just for syntax highlight
const Math = window.Math;
const MAX_PIXEL_PER_BRUSH_BP = 20;

export class Viewport extends BaseViewport {
    element: HTMLElement;
    brushChangeSubject = new Subject();
    shortenedIntronsChangeSubject = new Subject();

    margin = 0;

    nameGlobalEvent = 'viewport:position';
    nameEventObj = 'viewport';

    initialized = false;

    projectContext;

    onDestroy = null;

    _shortenedIntronsViewport = new ShortenedIntronsViewport(this);

    isSlave = false;

    constructor(element: HTMLElement, {
        chromosomeSize,
        brush = {
            end: chromosomeSize,
            start: 1
        }
    }, dispatcher, projectContext, margin = 0, browserInitialSetting) {
        super({
            brush: brush,
            canvas: {
                end: element.clientWidth - 2 * margin,
                start: 0
            },
            chromosome: {
                end: chromosomeSize,
                start: 1
            }
        });
        this.margin = margin;
        this.element = element;
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;

        if (browserInitialSetting && browserInitialSetting.browserId) {
            this.isSlave = true;
            this.nameGlobalEvent = `viewport:position:${browserInitialSetting.browserId}`;
            this.nameEventObj = `viewport:${browserInitialSetting.browserId}`;
        }

        const _nameGlobalEvent = this.nameGlobalEvent;
        const _nameEventObj = this.nameEventObj;
        this.dispatcher.emitGlobalEvent(this.nameGlobalEvent, {[this.nameEventObj]: this.brush});
        this.onDestroy = () => {
            this.dispatcher.emitGlobalEvent(_nameGlobalEvent, {[_nameEventObj]: null});
        };
        this._initSubscriptions();
        this.transform(brush);
        this.initialized = true;
    }

    get isShortenedIntronsMode() {
        return this._shortenedIntronsViewport.shortenedIntronsMode && this.shortenedIntronsViewport.brush;
    }

    get shortenedIntronsViewport(): ShortenedIntronsViewport {
        return this._shortenedIntronsViewport;
    }

    get factor() {
        if (this.isShortenedIntronsMode && this.shortenedIntronsViewport.brush) {
            return this.canvasSize / this.shortenedIntronsViewport.brush.shortenedSize;
        }
        return super.factor;
    }

    get actualBrushSize() {
        if (this.isShortenedIntronsMode && this.shortenedIntronsViewport.brush) {
            return this.shortenedIntronsViewport.brush.shortenedSize;
        }
        return this.brushSize;
    }

    get convert() {
        if (this.isShortenedIntronsMode && this.shortenedIntronsViewport.brush) {
            return this._shortenedConvert;
        }
        return super.convert;
    }

    get project() {
        if (this.isShortenedIntronsMode && this.shortenedIntronsViewport.brush) {
            return this._shortenedProject;
        }
        return super.project;
    }

    _shortenedConvert = {
        brushBP2pixel: bp => bp * this.factor,
        chromoBP2pixel: this.types.convert.bind(this, this.types.chromoBP, this.types.pixel),
        pixel2brushBP: px => px / this.factor,
        pixel2chromoBP: this.types.convert.bind(this, this.types.pixel, this.types.chromoBP)
    };

    _shortenedProject = {
        brushBP2pixel: bp =>
        (
            this.shortenedIntronsViewport.getShortenedRelativePosition(bp) -
            this.shortenedIntronsViewport.brush.relativeStartIndex
        ) * this.factor - this.canvas.start,
        chromoBP2pixel: this.types.project.bind(this, this.types.chromoBP, this.types.pixel),
        pixel2brushBP: px => this.shortenedIntronsViewport
            .translateStartPosition((px - this.canvas.start) / this.factor),
        pixel2chromoBP: this.types.project.bind(this, this.types.pixel, this.types.chromoBP)
    };

    _initSubscriptions() {
        angular.element(window).on('resize', () => {
            this.resize(this.element.clientWidth - 2 * this.margin);
        });

        let mouseIn = false;

        angular.element(this.element).on('mouseover.renderContainer', () => mouseIn = true);
        angular.element(this.element).on('mouseout.renderContainer', () => mouseIn = false);

        const zoomStepFactor = 10;

        KeyboardJS.on('ctrl + shift + =', () => {
            if (!mouseIn)
                return;
            const step = -this.brushSize / zoomStepFactor;
            this.transform({
                end: this.brush.end + step,
                start: this.brush.start - step
            });
        });
        KeyboardJS.on('ctrl + shift + -', () => {
            if (!mouseIn)
                return;
            const step = this.brushSize / zoomStepFactor;
            this.transform({
                end: this.brush.end + step,
                start: this.brush.start - step
            });
        });
    }

    get canTransform() {
        return !this.isShortenedIntronsMode;
    }

    get centerPosition() {
        if (this.isShortenedIntronsMode && this.shortenedIntronsViewport.brush) {
            return this.shortenedIntronsViewport.brush.center;
        }
        return (this.brush.start + this.brush.end) / 2;
    }

    transform({start = this.brush.start, end = this.brush.end, delta = 0}) {
        const oldBrush = {
            end: this.brush.end,
            start: this.brush.start,
        };
        if (this.convert.chromoBP2pixel(1) > MAX_PIXEL_PER_BRUSH_BP) {
            this.brush = {
                end: this.chromosome.end,
                start: this.chromosome.start
            };
        } else {
            const minBrushLength = this.canvasSize / MAX_PIXEL_PER_BRUSH_BP;

            const length = Math.max(minBrushLength, end - start);

            if (delta) {
                start += delta;
                start = Math.max(this.chromosome.start, start);
                end = Math.min(this.chromosome.end, start + length);
                start = Math.max(this.chromosome.start, end - length);
            }
            else {
                const center = (start + end) / 2;
                start = center - length / 2;
            }
            start = Math.max(this.chromosome.start, start);
            end = Math.min(this.chromosome.end, start + length);
            start = Math.max(this.chromosome.start, end - length);

            const normalize = (val) => Math.max(Math.min(val, this.chromosome.end), this.chromosome.start);

            this.brush = {
                end: normalize(end),
                start: normalize(start)
            };
        }

        if (this.shortenedIntronsViewport && this.shortenedIntronsViewport.shortenedIntronsMode) {
            (async () => {
                await this._shortenedIntronsViewport.transform({
                    delta,
                    end,
                    start,
                });
                if (!this.initialized || oldBrush.start !== this.brush.start || oldBrush.end !== this.brush.end) {
                    if (!this.isSlave) {
                        this.projectContext.changeState({[this.nameEventObj]: this.brush});
                    } else {
                        this.dispatcher.emitGlobalEvent(this.nameGlobalEvent, {[this.nameEventObj]: this.brush});
                    }
                    this.brushChangeSubject.onNext(this);
                }
            })();
        }
        else if (!this.initialized || oldBrush.start !== this.brush.start || oldBrush.end !== this.brush.end) {
            if (!this.isSlave) {
                this.projectContext.changeState({[this.nameEventObj]: this.brush});
            } else {
                this.dispatcher.emitGlobalEvent(this.nameGlobalEvent, {[this.nameEventObj]: this.brush});
            }
            this.brushChangeSubject.onNext(this);
        }
    }


    selectInterval(interval) {
        if (!interval) {
            return;
        }
        if (!this.canTransform) {
            if (!this.isSlave) {
                this.projectContext.changeState({[this.nameEventObj]: this.brush}, true);
            } else {
                this.dispatcher.emitGlobalEvent(this.nameGlobalEvent, {[this.nameEventObj]: this.brush});
            }
            return;
        }
        if (interval.position) {
            this.selectPosition(parseFloat(interval.position));
        } else if (interval.start && interval.end) {
            this.transform({
                end: parseFloat(interval.end),
                start: parseFloat(interval.start)
            });
        }
        else if (interval.start !== undefined && interval.start !== null) {
            const minBpLength = 100;
            const length = Math.max(this.canvasSize / MAX_PIXEL_PER_BRUSH_BP, minBpLength);
            this.transform({
                end: parseFloat(interval.start) + length / 2,
                start: parseFloat(interval.start) - length / 2
            });
        }
    }

    selectPosition(position) {
        if (!position) {
            return;
        }
        if (!this.canTransform) {
            if (!this.isSlave) {
                this.projectContext.changeState({[this.nameEventObj]: this.brush}, true);
            } else {
                this.dispatcher.emitGlobalEvent(this.nameGlobalEvent, {[this.nameEventObj]: this.brush});
            }
            return;
        }
        if (position === Object(position)) {
            return this.selectInterval(position);
        }
        position = parseInt(position);
        const length = this.brushSize;
        const brush = {
            end: position + length / 2,
            start: position - length / 2
        };
        this.transform(brush);
    }

    _formattedBrush() {
        let {start, end} = this.brush;
        start = parseInt(start);
        end = parseInt(end);
        return {
            end,
            start,
        };
    }
}
