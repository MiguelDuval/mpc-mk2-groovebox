package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Native-backed Track channels. Unsupported routing/FX destinations remain disabled. */
final class MpcChannelMixerView extends LinearLayout {
    enum Tab {
        VOLUME("VOLUME", true), PAN_VOLUME("PAN & VOLUME", true),
        SENDS("SENDS", false), EFFECTS("EFFECTS", false), IO("I/O", false);
        final String label;
        final boolean available;
        Tab(String label, boolean available) { this.label = label; this.available = available; }
    }
    interface Listener {
        void onSelectTrack(int index);
        float onTrackLevelChanged(int index, float value);
        float onTrackPanChanged(int index, float value);
        void onToggleTrackMute(int index);
        void onToggleTrackSolo(int index);
        void onTabChanged(Tab tab);
    }
    static final class TrackState {
        final int index;
        final String name;
        final String type;
        final float level;
        final float pan;
        final boolean muted, canSolo, soloed, audioEditable;
        TrackState(int index, String name, String type, float level, float pan,
                boolean muted, boolean canSolo, boolean soloed, boolean audioEditable) {
            this.index = index;
            this.name = name == null || name.trim().isEmpty()
                    ? String.format(Locale.ROOT, "TRACK %02d", index + 1) : name.trim();
            this.type = type == null ? "UNKNOWN" : type.toUpperCase(Locale.ROOT);
            this.level = MpcTrackMixerPolicy.clampLevel(level);
            this.pan = MpcTrackMixerPolicy.clampPan(pan);
            this.muted = muted;
            this.canSolo = canSolo;
            this.soloed = soloed;
            this.audioEditable = audioEditable;
        }
    }
    private static final int BG=Color.rgb(12,14,19), SURFACE=Color.rgb(25,28,36);
    private static final int SELECTED=Color.rgb(40,47,62), LINE=Color.rgb(68,74,88);
    private static final int TEXT=Color.rgb(236,238,243), MUTED=Color.rgb(132,140,154);
    private static final int ACCENT=Color.rgb(0,196,160);
    private final Listener listener;
    private final int density;
    private Tab currentTab=Tab.VOLUME;
    private HorizontalScrollView scroll;
    private int savedScrollX;

    MpcChannelMixerView(Context context, Listener listener) {
        super(context);
        this.listener=listener;
        density=Math.max(1,Math.round(getResources().getDisplayMetrics().density));
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setContentDescription("MPC Channel Mixer per-Track workspace");
    }

    void setState(List<TrackState> tracks,int selectedTrack,Tab tab) {
        currentTab=tab==null?Tab.VOLUME:tab;
        if(scroll!=null)savedScrollX=scroll.getScrollX();
        removeAllViews();
        LinearLayout titleRow=new LinearLayout(getContext());
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(dp(8),0,dp(8),0);
        titleRow.addView(label("CHANNEL MIXER",13,TEXT,true),
                new LayoutParams(0,dp(34),1f));
        String selectedName="—";
        if(tracks!=null)for(TrackState t:tracks)if(t.index==selectedTrack)selectedName=t.name;
        TextView selected=label(String.format(Locale.ROOT,"TRACK %02d • %s • %d TRACKS",
                selectedTrack+1,selectedName,tracks==null?0:tracks.size()),9,MUTED,true);
        selected.setContentDescription("MPC Channel Mixer selected Track "+(selectedTrack+1)+" "+selectedName);
        titleRow.addView(selected,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(34)));
        addView(titleRow,new LayoutParams(LayoutParams.MATCH_PARENT,dp(36)));

        LinearLayout tabs=new LinearLayout(getContext());
        tabs.setPadding(dp(4),dp(2),dp(4),dp(4));
        for(Tab tabItem:Tab.values()){
            final boolean active=tabItem==currentTab;
            TextView button=chip(tabItem.label,tabItem.available,active,
                    tabItem.available?"MPC Channel Mixer tab "+tabItem.label+(active?" selected":"")
                    :"MPC Channel Mixer "+tabItem.label+" unavailable: routing and effects backend not implemented");
            if(tabItem.available)button.setOnClickListener(v->{
                if(listener!=null&&tabItem!=currentTab)listener.onTabChanged(tabItem);
            });
            tabs.addView(button,new LayoutParams(0,dp(36),1f));
        }
        addView(tabs,new LayoutParams(LayoutParams.MATCH_PARENT,dp(42)));
        if(tracks==null||tracks.isEmpty()){
            TextView empty=label("NO TRACKS IN ACTIVE SEQUENCE",12,MUTED,true);
            empty.setGravity(Gravity.CENTER);
            addView(empty,new LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
            return;
        }
        if(!currentTab.available){
            TextView unavailable=label(currentTab.label+"\nNOT AVAILABLE IN THIS BUILD",13,MUTED,true);
            unavailable.setGravity(Gravity.CENTER);
            unavailable.setContentDescription("MPC Channel Mixer "+currentTab.label+" unavailable");
            addView(unavailable,new LayoutParams(LayoutParams.MATCH_PARENT,dp(64)));
        }
        scroll=new HorizontalScrollView(getContext());
        scroll.setHorizontalScrollBarEnabled(true);
        scroll.setFillViewport(true);
        scroll.setContentDescription("MPC Channel Mixer horizontally scrollable Track bank");
        LinearLayout row=new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setPadding(dp(3),dp(2),dp(3),dp(2));
        for(TrackState track:tracks){
            row.addView(buildTrack(track,track.index==selectedTrack),
                    new LayoutParams(dp(124),LayoutParams.MATCH_PARENT));
        }
        scroll.addView(row,new HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
        addView(scroll,new LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
        final int restore=savedScrollX;
        scroll.post(()->scroll.scrollTo(restore,0));
    }

    private View buildTrack(TrackState track,boolean selected) {
        LinearLayout strip=new LinearLayout(getContext());
        strip.setOrientation(VERTICAL);
        strip.setPadding(dp(3),dp(3),dp(3),dp(4));
        strip.setGravity(Gravity.CENTER_HORIZONTAL);
        strip.setBackground(background(selected?SELECTED:SURFACE,selected?ACCENT:LINE));
        strip.setContentDescription(String.format(Locale.ROOT,
                "MPC Channel Mixer Track %02d strip %s %s",track.index+1,track.type,track.name));

        TextView header=chip(String.format(Locale.ROOT,"%02d\n%s",track.index+1,shortName(track.name)),
                true,selected,"MPC Channel Mixer Track "+(track.index+1)+" header "+track.name);
        header.setTextSize(10); header.setMinHeight(dp(44));
        header.setOnClickListener(v->{if(listener!=null&&!selected)listener.onSelectTrack(track.index);});
        strip.addView(header,new LayoutParams(LayoutParams.MATCH_PARENT,dp(46)));
        TextView type=label(track.type,8,MUTED,true); type.setGravity(Gravity.CENTER);
        strip.addView(type,new LayoutParams(LayoutParams.MATCH_PARENT,dp(18)));

        LinearLayout ms=new LinearLayout(getContext());
        TextView mute=chip(track.muted?"M ON":"M",true,track.muted,
                "MPC Channel Mixer Track "+(track.index+1)+" Mute "+(track.muted?"ON":"OFF"));
        mute.setOnClickListener(v->{if(listener!=null)listener.onToggleTrackMute(track.index);});
        ms.addView(mute,new LayoutParams(0,dp(32),1f));
        TextView solo=chip(track.soloed?"S ON":"S",track.canSolo,track.soloed,
                track.canSolo?"MPC Channel Mixer Track "+(track.index+1)+" Solo "+(track.soloed?"ON":"OFF")
                :"MPC Channel Mixer Track "+(track.index+1)+" Solo unavailable: no active Drum playback route");
        if(track.canSolo)solo.setOnClickListener(v->{if(listener!=null)listener.onToggleTrackSolo(track.index);});
        ms.addView(solo,new LayoutParams(0,dp(32),1f));
        strip.addView(ms,new LayoutParams(LayoutParams.MATCH_PARENT,dp(34)));

        TextView level=label(String.format(Locale.ROOT,"LEVEL\n%3d%%",Math.round(track.level*100)),
                9,track.audioEditable?TEXT:MUTED,true);
        level.setGravity(Gravity.CENTER);
        level.setContentDescription("MPC Channel Mixer Track "+(track.index+1)+" Level "+Math.round(track.level*100)+" percent");
        strip.addView(level,new LayoutParams(LayoutParams.MATCH_PARENT,dp(30)));
        ChannelFader fader=new ChannelFader(getContext());
        fader.setValue(track.level);
        fader.setMixerEnabled(track.audioEditable&&currentTab.available);
        fader.setContentDescription("MPC Channel Mixer Track "+(track.index+1)+" Level fader "+Math.round(track.level*100)+" percent");
        fader.setListener(value->{
            if(listener==null)return value;
            float actual=listener.onTrackLevelChanged(track.index,value);
            level.setText(String.format(Locale.ROOT,"LEVEL\n%3d%%",Math.round(actual*100)));
            level.setContentDescription("MPC Channel Mixer Track "+(track.index+1)+" Level "+Math.round(actual*100)+" percent");
            fader.setContentDescription("MPC Channel Mixer Track "
                    +(track.index+1)+" Level fader "+Math.round(actual*100)+" percent");
            return actual;
        });
        strip.addView(fader,new LayoutParams(LayoutParams.MATCH_PARENT,0,1f));
        TextView scale=label("0     100     200",7,MUTED,false);scale.setGravity(Gravity.CENTER);
        strip.addView(scale,new LayoutParams(LayoutParams.MATCH_PARENT,dp(17)));

        if(currentTab==Tab.PAN_VOLUME){
            TextView panLabel=label("PAN • "+MpcTrackMixerPolicy.panLabel(track.pan),9,
                    track.audioEditable?TEXT:MUTED,true);
            panLabel.setGravity(Gravity.CENTER);
            panLabel.setContentDescription("MPC Channel Mixer Track "+(track.index+1)+" Pan "+MpcTrackMixerPolicy.panLabel(track.pan));
            strip.addView(panLabel,new LayoutParams(LayoutParams.MATCH_PARENT,dp(24)));
            ChannelPan pan=new ChannelPan(getContext());pan.setValue(track.pan);
            pan.setMixerEnabled(track.audioEditable);
            pan.setContentDescription("MPC Channel Mixer Track "+(track.index+1)+" Pan slider "+MpcTrackMixerPolicy.panLabel(track.pan));
            pan.setListener(value->{
                if(listener==null)return value;
                float actual=listener.onTrackPanChanged(track.index,value);
                panLabel.setText("PAN • "+MpcTrackMixerPolicy.panLabel(actual));
                panLabel.setContentDescription("MPC Channel Mixer Track "+(track.index+1)+" Pan "+MpcTrackMixerPolicy.panLabel(actual));
                pan.setContentDescription("MPC Channel Mixer Track "
                        +(track.index+1)+" Pan slider "+MpcTrackMixerPolicy.panLabel(actual));
                return actual;
            });
            strip.addView(pan,new LayoutParams(LayoutParams.MATCH_PARENT,dp(42)));
        }else{
            TextView pan=label("PAN • "+MpcTrackMixerPolicy.panLabel(track.pan),8,MUTED,false);
            pan.setGravity(Gravity.CENTER);
            strip.addView(pan,new LayoutParams(LayoutParams.MATCH_PARENT,dp(22)));
        }
        TextView route=label(track.audioEditable?"TRACK AUDIO":"AUDIO ROUTE\nNOT CONNECTED",
                7,track.audioEditable?ACCENT:MUTED,true);
        route.setGravity(Gravity.CENTER);
        if(!track.audioEditable)route.setContentDescription("MPC Channel Mixer Track "+(track.index+1)
                +" audio route unavailable: current audio engine has no Track voice route");
        strip.addView(route,new LayoutParams(LayoutParams.MATCH_PARENT,dp(track.audioEditable?24:30)));
        return strip;
    }

    private TextView chip(String value,boolean enabled,boolean selected,String description){
        TextView v=label(value,9,enabled?TEXT:MUTED,true);v.setGravity(Gravity.CENTER);
        v.setContentDescription(description);v.setEnabled(enabled);v.setFocusable(enabled);v.setClickable(enabled);
        v.setBackground(background(selected?Color.rgb(48,61,77):SURFACE,selected?ACCENT:LINE));return v;
    }
    private TextView label(String value,int size,int color,boolean bold){
        TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private GradientDrawable background(int fill,int stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(3));d.setStroke(dp(1),stroke);return d;
    }
    private String shortName(String value){return value.length()<=13?value:value.substring(0,12)+"…";}
    private int dp(float value){return Math.round(value*density);}
    private interface ValueListener{float change(float value);}

    private final class ChannelFader extends View{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private float value=1f;private boolean enabled;private ValueListener listener;
        ChannelFader(Context c){super(c);}
        void setValue(float v){value=MpcTrackMixerPolicy.clampLevel(v);invalidate();}
        void setMixerEnabled(boolean v){enabled=v;setFocusable(v);setClickable(v&&listener!=null);invalidate();}
        void setListener(ValueListener v){listener=v;setClickable(enabled&&v!=null);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight(),top=dp(8),bottom=Math.max(top+1,h-dp(8)),cx=w*.53f;
            paint.setColor(BG);c.drawRoundRect(new RectF(dp(17),top,w-dp(17),bottom),dp(3),dp(3),paint);
            paint.setColor(enabled?LINE:MUTED);paint.setStrokeWidth(dp(4));c.drawLine(cx,top,cx,bottom,paint);
            float y=bottom-(bottom-top)*(value/2f);paint.setColor(enabled?ACCENT:MUTED);
            c.drawRoundRect(new RectF(cx-dp(17),y-dp(5),cx+dp(17),y+dp(5)),dp(3),dp(3),paint);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(!enabled||listener==null)return false;
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:case MotionEvent.ACTION_MOVE:getParent().requestDisallowInterceptTouchEvent(true);update(e.getY());return true;
                case MotionEvent.ACTION_UP:update(e.getY());performClick();return true;
                case MotionEvent.ACTION_CANCEL:return true;default:return true;
            }
        }
        private void update(float y){
            float top=dp(8),bottom=Math.max(top+1,getHeight()-dp(8));
            float next=MpcTrackMixerPolicy.levelForTouch(y,top,bottom,2f);
            if(Math.abs(next-value)<.005f)return;value=MpcTrackMixerPolicy.clampLevel(listener.change(next));invalidate();
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }

    private final class ChannelPan extends View{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private float value;private boolean enabled;private ValueListener listener;
        ChannelPan(Context c){super(c);}
        void setValue(float v){value=MpcTrackMixerPolicy.clampPan(v);invalidate();}
        void setMixerEnabled(boolean v){enabled=v;setFocusable(v);setClickable(v&&listener!=null);invalidate();}
        void setListener(ValueListener v){listener=v;setClickable(enabled&&v!=null);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float cy=getHeight()/2f,left=getWidth()*.16f,right=getWidth()*.84f;
            paint.setColor(enabled?LINE:MUTED);paint.setStrokeWidth(dp(3));c.drawLine(left,cy,right,cy,paint);
            float center=(left+right)/2f;paint.setStrokeWidth(dp(1));c.drawLine(center,cy-dp(7),center,cy+dp(7),paint);
            float x=left+(right-left)*((value+1f)/2f);paint.setColor(enabled?ACCENT:MUTED);c.drawCircle(x,cy,dp(7),paint);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(!enabled||listener==null)return false;
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:case MotionEvent.ACTION_MOVE:getParent().requestDisallowInterceptTouchEvent(true);update(e.getX());return true;
                case MotionEvent.ACTION_UP:update(e.getX());performClick();return true;
                case MotionEvent.ACTION_CANCEL:return true;default:return true;
            }
        }
        private void update(float x){
            float left=getWidth()*.16f,right=getWidth()*.84f;
            float next=MpcTrackMixerPolicy.panForTouch(x,left,right);
            if(Math.abs(next-value)<.005f)return;value=MpcTrackMixerPolicy.clampPan(listener.change(next));invalidate();
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}