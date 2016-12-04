/*
 * Copyright 2016-present The Material Motion Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.motion.gestures;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A gesture recognizer generates continuous or discrete events from a stream of device input
 * events. When attached to an element, any interactions with that element will be interpreted by
 * the gesture recognizer and turned into gesture events. The output is often a linear
 * transformation of translation, rotation, and/or scale.
 * <p>
 * To use an instance of this class, first set the element with {@link #setElement(View)} then
 * forward all touch events from the element's parent to {@link #onTouchEvent(MotionEvent)}.
 */
public abstract class GestureRecognizer {

  /**
   * A listener that receives {@link GestureRecognizer} events.
   */
  public interface GestureStateChangeListener {

    /**
     * Notifies every time on {@link GestureRecognizerState state} change.
     * <p>
     * Implementations should query the provided gesture recognizer for its current state and
     * properties.
     *
     * @param gestureRecognizer the gesture recognizer where the event originated from.
     */
    void onStateChanged(GestureRecognizer gestureRecognizer);
  }

  /**
   * The gesture recognizer has not yet recognized its gesture, but may be evaluating touch
   * events. This is the default state.
   */
  public static final int POSSIBLE = 0;
  /**
   * The gesture recognizer has received touch objects recognized as a continuous gesture.
   */
  public static final int BEGAN = 1;
  /**
   * The gesture recognizer has received touches recognized as a change to a continuous gesture.
   */
  public static final int CHANGED = 2;
  /**
   * The gesture recognizer has received touches recognized as the end of a continuous gesture. At
   * the next cycle of the run loop, the gesture recognizer resets its state to {@link
   * #POSSIBLE}.
   */
  public static final int RECOGNIZED = 3;
  /**
   * The gesture recognizer has received touches resulting in the cancellation of a continuous
   * gesture. At the next cycle of the run loop, the gesture recognizer resets its state to {@link
   * #POSSIBLE}.
   */
  public static final int CANCELLED = 4;

  /**
   * The state of the gesture recognizer.
   */
  @IntDef({POSSIBLE, BEGAN, CHANGED, RECOGNIZED, CANCELLED})
  @Retention(RetentionPolicy.SOURCE)
  public @interface GestureRecognizerState {

  }

  protected static final int UNSET_SLOP = -1;

  /* Temporary variables. */
  private final Matrix matrix = new Matrix();
  private final float[] array = new float[2];
  private final PointF pointF = new PointF();

  /**
   * Inverse transformation matrix that is updated on a untransformed point calculation. Use this
   * to convert untransformed points back to the element's local coordinate system.
   */
  private final Matrix inverse = new Matrix();

  private final List<GestureStateChangeListener> listeners = new CopyOnWriteArrayList<>();
  @Nullable
  private View element;
  @GestureRecognizerState
  private int state = POSSIBLE;

  /**
   * Sets the view that this gesture recognizer is attached to. This must be called before this
   * gesture recognizer can start {@link #onTouchEvent(MotionEvent) accepting touch events}.
   */
  public void setElement(@Nullable View element) {
    this.element = element;
  }

  /**
   * Returns the view associated with this gesture recognizer.
   */
  public View getElement() {
    return element;
  }

  /**
   * Returns the current state of the gesture recognizer.
   */
  @GestureRecognizerState
  public int getState() {
    return state;
  }

  /**
   * Forwards touch events from a {@link OnTouchListener} to this gesture recognizer.
   */
  public abstract boolean onTouchEvent(MotionEvent event);

  /**
   * Adds a listener to this gesture recognizer.
   */
  public void addStateChangeListener(GestureStateChangeListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Removes a listener from this gesture recognizer.
   */
  public void removeStateChangeListener(GestureStateChangeListener listener) {
    listeners.remove(listener);
  }

  /**
   * Returns the centroidX position of the current gesture in the local coordinate space of the
   * {@link #element}.
   */
  public final float getCentroidX() {
    array[0] = getUntransformedCentroidX();
    array[1] = getUntransformedCentroidY();

    inverse.mapPoints(array);

    return array[0];
  }

  /**
   * Returns the centroidY position of the current gesture in the local coordinate space of the
   * {@link #element}.
   */
  public final float getCentroidY() {
    array[0] = getUntransformedCentroidX();
    array[1] = getUntransformedCentroidY();

    inverse.mapPoints(array);

    return array[1];
  }

  /**
   * Returns the untransformed centroidX position of the current gesture in the local coordinate
   * space of {@link #element}'s parent.
   */
  public abstract float getUntransformedCentroidX();

  /**
   * Returns the untransformed centroidY position of the current gesture in the local coordinate
   * space of {@link #element}'s parent.
   */
  public abstract float getUntransformedCentroidY();

  /**
   * Sets the state of the gesture recognizer and notifies all listeners.
   */
  protected void setState(@GestureRecognizerState int state) {
    this.state = state;

    for (GestureStateChangeListener listener : listeners) {
      listener.onStateChanged(this);
    }

    element.removeCallbacks(setStateToPossible);
    if (state == RECOGNIZED || state == CANCELLED) {
      element.post(setStateToPossible);
    }
  }

  private final Runnable setStateToPossible = new Runnable() {
    @Override
    public void run() {
      setState(POSSIBLE);
    }
  };

  protected boolean isInProgress() {
    return state == BEGAN || state == CHANGED;
  }

  /**
   * Calculates the untransformed centroid of all the active pointers in the given motion event.
   *
   * @return A point representing the centroid. The caller should read the values immediately as
   * the object may be reused in other calculations.
   */
  protected PointF calculateUntransformedCentroid(MotionEvent event) {
    return calculateUntransformedCentroid(event, Integer.MAX_VALUE);
  }

  /**
   * Calculates the centroid of the first {@code n} active pointers in the given motion event.
   *
   * @return A point representing the centroid. The caller should read the values immediately as
   * the object may be reused in other calculations.
   */
  protected PointF calculateUntransformedCentroid(MotionEvent event, int n) {
    int action = MotionEventCompat.getActionMasked(event);
    int index = MotionEventCompat.getActionIndex(event);

    float sumX = 0;
    float sumY = 0;
    int num = 0;
    for (int i = 0, count = event.getPointerCount(); i < count && i < n; i++) {
      if (action == MotionEvent.ACTION_POINTER_UP && index == i) {
        continue;
      }

      sumX += calculateUntransformedPoint(event, i).x;
      sumY += calculateUntransformedPoint(event, i).y;
      num++;
    }

    pointF.set(sumX / num, sumY / num);
    return pointF;
  }

  /**
   * Calculates the untransformed x and y of the pointer given by the pointer index in the given
   * motion event.
   * <p>
   * An untransformed coordinate represents the location of a pointer that is not transformed by
   * the element's transformation matrix. {@code calculateUntransformedPoint(event, 0).x} is not
   * necessarily equal to {@code event.getRawX()}.
   *
   * @return A point representing the untransformed x and y. The caller should read the values
   * immediately as the object may be reused in other calculations.
   */
  protected PointF calculateUntransformedPoint(MotionEvent event, int pointerIndex) {
    array[0] = event.getX(pointerIndex);
    array[1] = event.getY(pointerIndex);

    getTransformationMatrix(element, matrix, inverse);
    matrix.mapPoints(array);
    pointF.set(array[0], array[1]);

    return pointF;
  }

  /**
   * Calculates the transformation matrices that can convert from local to untransformed
   * coordinate spaces.
   *
   * @param matrix This output matrix can convert from local to untransformed coordinate space.
   * @param inverse This output matrix can convert from untransformed to local coordinate space.
   */
  public static void getTransformationMatrix(View element, Matrix matrix, Matrix inverse) {
    matrix.reset();
    matrix.postScale(
      element.getScaleX(), element.getScaleY(), element.getPivotX(), element.getPivotY());
    matrix.postRotate(element.getRotation(), element.getPivotX(), element.getPivotY());
    matrix.postTranslate(element.getTranslationX(), element.getTranslationY());

    // Save the inverse matrix.
    matrix.invert(inverse);
  }
}
