package org.metawatch.manager.stocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;

public class Widget extends BroadcastReceiver  {
	
	public final static String id_prefix = "stocksMWM_";  
	public final static String id_suffix = "_32_13";  
	final static String desc_suffix = " stock symbol (32x13)";

	static Typeface typeface = null;
	static Typeface typefaceNumerals = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Stocks.TAG, "onReceive()");
		
		HashMap<String, List<String>> stockData = Stocks.getCached(context);
		if (stockData==null) {
			Log.d(Stocks.TAG, "No stocks data.");
			return;
		}

		Log.d(Stocks.TAG, "Cache data contains "+stockData.size()+" entries");
		
		String action = intent.getAction();
		if (action !=null && action.equals("org.metawatch.manager.REFRESH_WIDGET_REQUEST")) {

			Log.d(Stocks.TAG, "Received intent");

			Bundle bundle = intent.getExtras();

			boolean getPreviews = bundle.containsKey("org.metawatch.manager.get_previews");
			if (getPreviews)
				Log.d(Stocks.TAG, "get_previews");

			ArrayList<String> widgets_desired = null;

			if (bundle.containsKey("org.metawatch.manager.widgets_desired")) {
				Log.d(Stocks.TAG, "widgets_desired");
				widgets_desired = new ArrayList<String>(Arrays.asList(bundle.getStringArray("org.metawatch.manager.widgets_desired")));
			}

			for(HashMap.Entry<String, List<String>> entry : stockData.entrySet()) {
				boolean active = (widgets_desired!=null && widgets_desired.contains(entry.getKey()));
				if (getPreviews || active) {
					genWidget(context, entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	public synchronized static void update(Context context, HashMap<String, List<String>> data) {
		Log.d(Stocks.TAG, "Updating widget");
		
		for(HashMap.Entry<String, List<String>> entry : data.entrySet()) {
			genWidget(context, entry.getKey(), entry.getValue());
		}
	}

	private synchronized static void genWidget(Context context, String id, List<String> values) {
		Log.d(Stocks.TAG, "genWidget() start");
		
		if (typeface==null) {
			typeface = Typeface.createFromAsset(context.getAssets(), "metawatch_8pt_5pxl_CAPS.ttf");
		}
		if (typefaceNumerals==null) {
			typefaceNumerals = Typeface.createFromAsset(context.getAssets(), "metawatch_8pt_5pxl_Numerals.ttf");
		}
		
		TextPaint paint = new TextPaint();
		paint.setColor(Color.BLACK);
		paint.setTextSize(8);
		paint.setTypeface(typeface);
		paint.setTextAlign(Align.CENTER);
		
		TextPaint paintNumerals = new TextPaint();
		paintNumerals.setColor(Color.BLACK);
		paintNumerals.setTextSize(8);
		paintNumerals.setTypeface(typefaceNumerals);
		paintNumerals.setTextAlign(Align.CENTER);
		
		final int width = 32;
		final int height = 13;

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		
		String symbol = (String) TextUtils.ellipsize(values.get(0), paint, width-2, TruncateAt.END);
		
		String val = values.get(1);
		if(val.contains(".") && paint.measureText(val) > width)
			val = val.substring(0, val.indexOf("."));
		
		canvas.drawText(symbol, width/2, 6, paint);
		canvas.drawText(val, width/2, 12, paintNumerals);
		 	
		Intent i = createUpdateIntent(bitmap, id, values.get(0) + desc_suffix, 1);
		context.sendBroadcast(i);

		Log.d(Stocks.TAG, "genWidget() end");
	}

	
	/**
	 * @param bitmap Widget image to send
	 * @param id ID of this widget - should be unique, and sensibly identify
	 *        the widget
	 * @param description User friendly widget name (will be displayed in the
	 * 		  widget picker)
	 * @param priority A value that indicates how important this widget is, for
	 * 		  use when deciding which widgets to discard.  Lower values are
	 *        more likely to be discarded.
	 * @return Filled-in intent, ready for broadcast.
	 */
	private static Intent createUpdateIntent(Bitmap bitmap, String id, String description, int priority) {
		int pixelArray[] = new int[bitmap.getWidth() * bitmap.getHeight()];
		bitmap.getPixels(pixelArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		Intent intent = new Intent("org.metawatch.manager.WIDGET_UPDATE");
		Bundle b = new Bundle();
		b.putString("id", id);
		b.putString("desc", description);
		b.putInt("width", bitmap.getWidth());
		b.putInt("height", bitmap.getHeight());
		b.putInt("priority", priority);
		b.putIntArray("array", pixelArray);
		intent.putExtras(b);

		return intent;
	}

}
