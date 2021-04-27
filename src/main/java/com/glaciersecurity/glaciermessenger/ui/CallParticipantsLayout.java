package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.R;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexboxLayout;

import java.util.Collections;
import java.util.List;

/**
 * Can dynamically render a collection of call participants, adjusting their
 * sizing and layout depending on the total number of participants.
 */
public class CallParticipantsLayout extends FlexboxLayout {

  private static final int MULTIPLE_PARTICIPANT_SPACING = dpToPx(3);
  private static final int CORNER_RADIUS                = dpToPx(10);

  private List<TwilioCallParticipant> callParticipants = Collections.emptyList();

  public CallParticipantsLayout(@NonNull Context context) {
    super(context);
  }

  public CallParticipantsLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public CallParticipantsLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void update(@NonNull List<TwilioCallParticipant> callParticipants){//, @NonNull RemoteParticipant focusedParticipant), boolean shouldRenderInPip) {
    this.callParticipants   = callParticipants;
    updateLayout();
  }

  public static int dpToPx(int dp) {
    return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
  }

  public static int getStatusBarHeight(@NonNull View view) {
    int result = 0;
    int resourceId = view.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      result = view.getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }

  private void updateLayout() {
    int previousChildCount = getChildCount();

    int count = callParticipants.size();
    updateChildrenCount(count);

    for (int i = 0; i < count; i++) {
      update(i, count, callParticipants.get(i));
    }

    if (previousChildCount != getChildCount()) {
      updateMarginsForLayout();
    }
  }

  private void updateMarginsForLayout() {
    MarginLayoutParams layoutParams = (MarginLayoutParams) getLayoutParams();
    if (callParticipants.size() > 1) { // && !shouldRenderInPip) {
      layoutParams.setMargins(MULTIPLE_PARTICIPANT_SPACING, 0, MULTIPLE_PARTICIPANT_SPACING, 0);
    } else {
      layoutParams.setMargins(0, 0, 0, 0);
    }
    setLayoutParams(layoutParams);
  }

  private void updateChildrenCount(int count) {
      int childCount = getChildCount();
      if (childCount < count) {
          for (int i = childCount; i < count; i++) {
              addCallParticipantView();
          }
      } else if (childCount > count) {
          for (int counter = childCount - 1; counter >= 0; counter--) {
              CallParticipantView callParticipantView = getChildAt(counter).findViewById(R.id.group_call_participant);
              TwilioCallParticipant callParticipant = callParticipantView.getCallParticipant();
              boolean found = false;
              for (int j = 0; j < callParticipants.size(); j++) {
                  if (callParticipants.get(j).equals(callParticipant)) {
                      found = true;
                      break;
                  }
              }
              if (!found) {
                  callParticipantView.cleanupCallParticipant();
                  removeViewAt(counter);
              }
          }
      }
  }

  private void update(int index, int count, @NonNull TwilioCallParticipant participant) {
    View                view                = getChildAt(index);
    CardView            cardView            = view.findViewById(R.id.group_call_participant_card_wrapper);
    CallParticipantView callParticipantView = view.findViewById(R.id.group_call_participant);

    callParticipantView.setCallParticipant(participant);

    if (count > 1) {
      view.setPadding(MULTIPLE_PARTICIPANT_SPACING, MULTIPLE_PARTICIPANT_SPACING, MULTIPLE_PARTICIPANT_SPACING, MULTIPLE_PARTICIPANT_SPACING);
      cardView.setRadius(CORNER_RADIUS);
    } else {
      view.setPadding(0, 0, 0, 0);
      cardView.setRadius(0);
    }

    setChildLayoutParams(view, index, getChildCount());
  }

  private void addCallParticipantView() {
    View                       view   = LayoutInflater.from(getContext()).inflate(R.layout.group_call_participant_item, this, false);
    FlexboxLayout.LayoutParams params = (FlexboxLayout.LayoutParams) view.getLayoutParams();

    params.setAlignSelf(AlignItems.STRETCH);
    view.setLayoutParams(params);
    addView(view);
  }

  private void setChildLayoutParams(@NonNull View child, int childPosition, int childCount) {
    FlexboxLayout.LayoutParams params = (FlexboxLayout.LayoutParams) child.getLayoutParams();
    if (childCount < 3) {
      params.setFlexBasisPercent(1f);
    } else {
      if ((childCount % 2) != 0 && childPosition == childCount - 1) {
        params.setFlexBasisPercent(1f);
      } else {
        params.setFlexBasisPercent(0.5f);
      }
    }

    int height = this.getHeight();
    if (childCount > 1 && height > 0) {
      params.setMaxHeight(height/2);
    } else {
      params.setMaxHeight(height);
    }

    child.setLayoutParams(params);
  }
}
