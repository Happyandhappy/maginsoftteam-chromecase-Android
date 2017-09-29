package com.oxycast.chromecastapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cleveroad.slidingtutorial.Direction;
import com.cleveroad.slidingtutorial.IndicatorOptions;
import com.cleveroad.slidingtutorial.PageOptions;
import com.cleveroad.slidingtutorial.TransformItem;
import com.cleveroad.slidingtutorial.TutorialFragment;
import com.cleveroad.slidingtutorial.TutorialOptions;
import com.cleveroad.slidingtutorial.TutorialPageOptionsProvider;
import com.cleveroad.slidingtutorial.TutorialSupportFragment;
import com.oxycast.chromecastapp.R;

public class AppIntroActivity extends AppCompatActivity {
	private static final int TOTAL_PAGES = 3;
	private static final int ACTUAL_PAGES_COUNT = 3;
	private int[] mPagesColors;
	private TextView textViewDone;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_intro);
		textViewDone= (TextView) findViewById(R.id.tvDone);
		textViewDone.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent=new Intent(AppIntroActivity.this,MainActivity.class);
				intent.setAction("");
				startActivity(intent);
				finish();
			}
		});
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("introDone",true);
		editor.commit();
		mPagesColors = new int[]{
				ContextCompat.getColor(this, R.color.colorPrimary),
				ContextCompat.getColor(this, R.color.colorPrimary),
				ContextCompat.getColor(this, R.color.colorPrimary)
		};
		if (savedInstanceState==null)
		replaceTutorialFragment();
	}

	public void replaceTutorialFragment() {
		final IndicatorOptions indicatorOptions = IndicatorOptions.newBuilder(this)
				.build();
		final TutorialOptions tutorialOptions = TutorialSupportFragment.newTutorialOptionsBuilder(this)
				.setUseAutoRemoveTutorialFragment(false)
				.setUseInfiniteScroll(false)
				.setPagesColors(mPagesColors)
				.setPagesCount(TOTAL_PAGES)
				.setIndicatorOptions(indicatorOptions)
				.setTutorialPageProvider(new TutorialPagesProvider())
				.setOnSkipClickListener(new OnSkipClickListener(this))
				.build();
		final TutorialSupportFragment  tutorialFragment = TutorialSupportFragment .newInstance(tutorialOptions);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container, tutorialFragment)
				.commit();
	}

	private  final class TutorialPagesProvider implements TutorialPageOptionsProvider {

		@NonNull
		@Override
		public PageOptions provide(int position) {
			@LayoutRes int pageLayoutResId;
			TransformItem[] tutorialItems;
			position %= ACTUAL_PAGES_COUNT;
			if (position==2){
				textViewDone.setVisibility(View.VISIBLE);
			}
			switch (position) {
				case 0: {
					pageLayoutResId = R.layout.fragment_intro1;
					tutorialItems = new TransformItem[]{
							TransformItem.create(R.id.main, Direction.LEFT_TO_RIGHT, 0.20f),
						};
					break;
				}
				case 1: {
					pageLayoutResId = R.layout.fragment_intro2;
					tutorialItems = new TransformItem[]{
							TransformItem.create(R.id.main, Direction.LEFT_TO_RIGHT, 0.20f),
						};
					break;
				}
				case 2: {
					pageLayoutResId = R.layout.fragment_intro3;
					tutorialItems = new TransformItem[]{
							TransformItem.create(R.id.main, Direction.LEFT_TO_RIGHT, 0.2f),
						};
					break;
				}
				default: {
					throw new IllegalArgumentException("Unknown position: " + position);
				}
			}

			return PageOptions.create(pageLayoutResId, position, tutorialItems);
		}
	}

	private  final class OnSkipClickListener implements View.OnClickListener {

		@NonNull
		private final Context mContext;

		OnSkipClickListener(@NonNull Context context) {
			mContext = context.getApplicationContext();
		}

		@Override
		public void onClick(View v) {
			Intent intent=new Intent(AppIntroActivity.this,MainActivity.class);
			intent.setAction("");
			startActivity(intent);
			finish();
		}
	}
}