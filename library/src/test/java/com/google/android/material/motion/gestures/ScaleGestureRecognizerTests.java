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

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.android.material.motion.gestures.GestureRecognizer.BEGAN;
import static com.google.android.material.motion.gestures.GestureRecognizer.CANCELLED;
import static com.google.android.material.motion.gestures.GestureRecognizer.CHANGED;
import static com.google.android.material.motion.gestures.GestureRecognizer.POSSIBLE;
import static com.google.android.material.motion.gestures.GestureRecognizer.RECOGNIZED;
import static com.google.android.material.motion.gestures.ScaleGestureRecognizer.dist;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ScaleGestureRecognizerTests {

  private static final float E = 0.0001f;

  private View element;
  private ScaleGestureRecognizer scaleGestureRecognizer;

  private long eventDownTime;
  private long eventTime;

  @Before
  public void setUp() {
    Context context = Robolectric.setupActivity(Activity.class);
    element = new View(context);
    scaleGestureRecognizer = new ScaleGestureRecognizer();
    scaleGestureRecognizer.setElement(element);
    scaleGestureRecognizer.scaleSlop = 0;

    eventDownTime = 0;
    eventTime = -16;
  }

  @Test
  public void defaultState() {
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(scaleGestureRecognizer.getElement()).isEqualTo(element);
    assertThat(scaleGestureRecognizer.getUntransformedCentroidX()).isWithin(0).of(0f);
    assertThat(scaleGestureRecognizer.getUntransformedCentroidY()).isWithin(0).of(0f);
    assertThat(scaleGestureRecognizer.getScale()).isWithin(0).of(1f);
    assertThat(scaleGestureRecognizer.getVelocity()).isWithin(0).of(0f);
  }

  @Test
  public void smallMovementIsNotRecognized() {
    scaleGestureRecognizer.scaleSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // First finger down.
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Second finger down.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move second finger 1 pixel. Should not change the state.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 101, 100));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void largeHorizontalMovementIsRecognized() {
    scaleGestureRecognizer.scaleSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // First finger down.
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Second finger down.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move second finger 100 pixel right. Should change the state.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 200, 100));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED});

    // Move second finger 1 pixel. Should still change the state.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 201, 100));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CHANGED});
  }

  @Test
  public void largeVerticalMovementIsRecognized() {
    scaleGestureRecognizer.scaleSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // First finger down.
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Second finger down.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    // Move second finger 100 pixel down. Should change the state.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, 200));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED});

    // Move second finger 1 pixel. Should still change the state.
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100, 201));
    assertThat(scaleGestureRecognizer.getState()).isEqualTo(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CHANGED});
  }

  @Test
  public void completedGestureIsRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 2, 0, 0, 100, 100, 200, 200));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 200, 100, 200, 200));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 2, 0, 0, 200, 100, 200, 200));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 200, 100));
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, 0, 0));

    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray())
      .isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, RECOGNIZED, POSSIBLE});
  }

  @Test
  public void cancelledOneFingerGestureIsNotRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_MOVE, 100, 0));
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_CANCEL, 100, 0));

    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void cancelledTwoFingerGestureIsNotRecognized() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 200, 100));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_CANCEL, 0, 0, 0, 200, 100));

    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray())
      .isEqualTo(new Integer[]{POSSIBLE, BEGAN, CHANGED, CANCELLED, POSSIBLE});
  }

  @Test
  public void noMovementIsNotRecognized() {
    scaleGestureRecognizer.scaleSlop = 24;

    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 100, 100));
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, 0, 0));

    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void irrelevantMotionIsIgnored() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);

    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_HOVER_MOVE, 0, 0));

    assertThat(scaleGestureRecognizer.getState()).isEqualTo(POSSIBLE);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});
  }

  @Test
  public void oneFingerDoesNotAffectScale() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);

    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(scaleGestureRecognizer.getScale()).isWithin(E).of(1f);

    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_MOVE, 100, 100));
    assertThat(scaleGestureRecognizer.getScale()).isWithin(E).of(1f);
  }

  @Test
  public void multitouchHasCorrectCentroidAndScale() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    scaleGestureRecognizer.addStateChangeListener(listener);

    // First finger down. Centroid is at finger location and scale is 1.
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    assertThat(scaleGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(0);
    assertThat(scaleGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(0);
    assertThat(scaleGestureRecognizer.getScale()).isWithin(E).of(1);

    // Second finger down. Centroid is in between fingers and scale is 1.
    scaleGestureRecognizer.onTouchEvent(
      createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 100, 100));
    assertThat(scaleGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(50);
    assertThat(scaleGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(50);
    assertThat(scaleGestureRecognizer.getScale()).isWithin(E).of(1);

    // Second finger moves [dx, dy]. Centroid moves [dx/2, dy/2], scale is calculated correctly.
    float dx = 505;
    float dy = 507;
    scaleGestureRecognizer.onTouchEvent(
      createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 100 + dx, 100 + dy));
    assertThat(scaleGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(50 + dx / 2);
    assertThat(scaleGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(50 + dy / 2);
    assertThat(scaleGestureRecognizer.getScale()).isWithin(E).of(
      dist(0, 0, 100 + dx, 100 + dy) / dist(0, 0, 100, 100));

    // Second finger up. State is now reset.
    scaleGestureRecognizer.onTouchEvent(
      createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 100 + dx, 100 + dy));
    assertThat(scaleGestureRecognizer.getUntransformedCentroidX()).isWithin(E).of(0);
    assertThat(scaleGestureRecognizer.getUntransformedCentroidY()).isWithin(E).of(0);
    assertThat(scaleGestureRecognizer.getScale()).isWithin(E).of(1);

    assertThat(listener.states.toArray()).isEqualTo(
      new Integer[]{POSSIBLE, BEGAN, CHANGED, RECOGNIZED, POSSIBLE});
  }

  @Test
  public void nonZeroVelocity() {
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_DOWN, 1, 0, 0, 10, 0));

    float move = 0;
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 10 + (move += 10), 0));
    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_MOVE, 1, 0, 0, 10 + (move += 10), 0));

    scaleGestureRecognizer.onTouchEvent(createMultiTouchMotionEvent(MotionEvent.ACTION_POINTER_UP, 1, 0, 0, 10 + move, 0));
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_UP, 0, 0));

    assertThat(scaleGestureRecognizer.getVelocity()).isGreaterThan(0f);
  }

  @Test(expected = NullPointerException.class)
  public void crashesForNullElement() {
    scaleGestureRecognizer.setElement(null);
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
  }

  @Test
  public void allowsSettingElementAgain() {
    scaleGestureRecognizer.setElement(new View(element.getContext()));
    scaleGestureRecognizer.onTouchEvent(createMotionEvent(MotionEvent.ACTION_DOWN, 0, 0));
  }

  private MotionEvent createMotionEvent(int action, float x, float y) {
    return MotionEvent.obtain(eventDownTime, eventTime += 16, action, x, y, 0);
  }

  private MotionEvent createMultiTouchMotionEvent(
    int action, int index, float x0, float y0, float x1, float y1) {
    MotionEvent event = mock(MotionEvent.class);

    when(event.getDownTime()).thenReturn(eventDownTime);
    when(event.getEventTime()).thenReturn(eventTime += 16);

    when(event.getPointerCount()).thenReturn(2);
    when(event.getAction()).thenReturn(action | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
    when(event.getActionMasked()).thenReturn(action);
    when(event.getActionIndex()).thenReturn(index);

    when(event.getRawX()).thenReturn(x0);
    when(event.getRawY()).thenReturn(y0);

    when(event.getX(0)).thenReturn(x0);
    when(event.getY(0)).thenReturn(y0);

    when(event.getX(1)).thenReturn(x1);
    when(event.getY(1)).thenReturn(y1);

    return event;
  }

  private MotionEvent createMultiTouchMotionEvent(
    int action, int index, float x0, float y0, float x1, float y1, float x2, float y2) {
    MotionEvent event = mock(MotionEvent.class);

    when(event.getDownTime()).thenReturn(eventDownTime);
    when(event.getEventTime()).thenReturn(eventTime += 16);

    when(event.getPointerCount()).thenReturn(3);
    when(event.getAction()).thenReturn(action | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
    when(event.getActionMasked()).thenReturn(action);
    when(event.getActionIndex()).thenReturn(index);

    when(event.getRawX()).thenReturn(x0);
    when(event.getRawY()).thenReturn(y0);

    when(event.getX(0)).thenReturn(x0);
    when(event.getY(0)).thenReturn(y0);

    when(event.getX(1)).thenReturn(x1);
    when(event.getY(1)).thenReturn(y1);

    when(event.getX(2)).thenReturn(x2);
    when(event.getY(2)).thenReturn(y2);

    return event;
  }
}
