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
import android.view.View;

import com.google.android.material.motion.gestures.testing.SimulatedGestureRecognizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.android.material.motion.gestures.GestureRecognizer.BEGAN;
import static com.google.android.material.motion.gestures.GestureRecognizer.CHANGED;
import static com.google.android.material.motion.gestures.GestureRecognizer.POSSIBLE;
import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class GestureRecognizerTests {

  private SimulatedGestureRecognizer gestureRecognizer;

  @Before
  public void setUp() {
    View element = new View(Robolectric.setupActivity(Activity.class));
    gestureRecognizer = new SimulatedGestureRecognizer(element);
  }

  @Test
  public void removedListenerDoesNotGetEvents() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();
    gestureRecognizer.addStateChangeListener(listener);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    gestureRecognizer.setState(BEGAN);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN});

    gestureRecognizer.removeStateChangeListener(listener);
    gestureRecognizer.setState(CHANGED);
    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN});

  }

  @Test
  public void addingSameListenerTwiceDoesNotSendTwoEvents() {
    TrackingGestureStateChangeListener listener = new TrackingGestureStateChangeListener();

    gestureRecognizer.addStateChangeListener(listener);
    gestureRecognizer.addStateChangeListener(listener);

    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE});

    gestureRecognizer.setState(BEGAN);

    assertThat(listener.states.toArray()).isEqualTo(new Integer[]{POSSIBLE, BEGAN});
  }

  @Test
  public void canSetNullElement() {
    gestureRecognizer.setElement(null);
  }
}
