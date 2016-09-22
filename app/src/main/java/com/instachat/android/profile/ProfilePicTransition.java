package com.instachat.android.profile;

import android.annotation.TargetApi;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.TransitionSet;

/**
 * Created by kevin on 9/13/2016.
 */
@TargetApi(21)
public class ProfilePicTransition extends TransitionSet {
    public ProfilePicTransition() {
        setOrdering(ORDERING_TOGETHER);
        addTransition(new ChangeBounds()).
                addTransition(new ChangeTransform()).
                addTransition(new ChangeImageTransform());
    }
}
