package br.org.servitas.familia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One rendered page at a time bounds memory usage; Android supplies the PDF engine. */
public final class LocalPdfActivity extends Activity {
    private PdfRenderer renderer;
    private PageView pageView;
    private TextView position;
    private ImageButton previous,next;
    private int pageIndex;
    private static final int INK=0xff26384a,BORDER=0xffe2e6e9,DISABLED=0xffb8c0c7,BAR=0xfffcfbf8;
    private static final int BACK=0,NEXT=1,MINUS=2,PLUS=3,PRINT=4;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private ImageButton button(int icon,String description,String tag){
        ImageButton button=new ImageButton(this);button.setImageDrawable(new ControlIcon(icon));
        button.setContentDescription(description);button.setTag(tag);
        button.setMinimumHeight(dp(48));button.setMinimumWidth(dp(48));
        GradientDrawable surface=new GradientDrawable();surface.setColor(Color.WHITE);
        surface.setCornerRadius(dp(12));surface.setStroke(dp(1),BORDER);
        GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(12));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1826384a),new InsetDrawable(surface,dp(4)),new InsetDrawable(mask,dp(4))));
        button.setPadding(0,0,0,0);button.setElevation(0);button.setStateListAnimator(null);button.setBackgroundTintList(null);button.setImageTintList(null);button.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        if(android.os.Build.VERSION.SDK_INT>=26)button.setTooltipText(description);
        return button;
    }
    private void divider(LinearLayout root){
        View line=new View(this);line.setBackgroundColor(BORDER);
        root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private final class ControlIcon extends Drawable {
        private final int icon;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ControlIcon(int value){icon=value;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        @Override public int getIntrinsicWidth(){return dp(24);}
        @Override public int getIntrinsicHeight(){return dp(24);}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
        @Override public void draw(Canvas canvas){
            boolean enabled=false;for(int state:getState())if(state==android.R.attr.state_enabled)enabled=true;
            paint.setColor(enabled?INK:DISABLED);
            int save=canvas.save();canvas.translate(getBounds().left,getBounds().top);
            canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
            Path path=new Path();
            if(icon==BACK){path.moveTo(15,5);path.lineTo(8,12);path.lineTo(15,19);}
            else if(icon==NEXT){path.moveTo(9,5);path.lineTo(16,12);path.lineTo(9,19);}
            else if(icon==PRINT){path.moveTo(7,8);path.lineTo(7,3);path.lineTo(17,3);path.lineTo(17,8);path.moveTo(7,17);path.lineTo(3,17);path.lineTo(3,8);path.lineTo(21,8);path.lineTo(21,17);path.lineTo(17,17);path.moveTo(7,13);path.lineTo(17,13);path.lineTo(17,21);path.lineTo(7,21);path.close();path.moveTo(17,10.5f);path.lineTo(18,10.5f);}
            else {path.moveTo(5,12);path.lineTo(19,12);if(icon==PLUS){path.moveTo(12,5);path.lineTo(12,19);}}
            canvas.drawPath(path,paint);canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int value){paint.setAlpha(value);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        try {
            renderer=LocalPdfStore.open(this,getIntent().getStringExtra("path"));
            pageIndex=Math.max(0,Math.min(renderer.getPageCount()-1,state==null?getIntent().getIntExtra("page",1)-1:state.getInt("page")));
            LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
            // Respect display cutouts, gesture navigation and system bars on API 35+.
            root.setOnApplyWindowInsetsListener((view,insets)->{
                if(android.os.Build.VERSION.SDK_INT>=30){
                    android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
                    view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
                }else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
                return insets;
            });
            LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setBackgroundColor(BAR);toolbar.setPadding(dp(4),0,dp(4),0);
            ImageButton close=button(BACK,"Voltar ao aplicativo","pdf-close");close.setOnClickListener(view->finish());toolbar.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));
            TextView title=new TextView(this);title.setText(getIntent().getStringExtra("title"));title.setTextColor(INK);title.setTextSize(15);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);title.setGravity(Gravity.CENTER_VERTICAL);title.setPadding(dp(8),0,dp(8),0);title.setTypeface(Typeface.create("serif",Typeface.NORMAL));
            toolbar.addView(title,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
            ImageButton less=button(MINUS,"Diminuir ampliação","pdf-zoom-out"),more=button(PLUS,"Aumentar ampliação","pdf-zoom-in");
            toolbar.addView(less,new LinearLayout.LayoutParams(dp(48),dp(48)));toolbar.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));
            ImageButton print=button(PRINT,"Imprimir PDF","pdf-print");
            print.setOnClickListener(view->printPdf());toolbar.addView(print,new LinearLayout.LayoutParams(dp(48),dp(48)));
            root.addView(toolbar);
            divider(root);
            pageView=new PageView();pageView.setTag("pdf-page");root.addView(pageView,new LinearLayout.LayoutParams(-1,0,1));
            less.setOnClickListener(view->pageView.zoomBy(1/1.25f));more.setOnClickListener(view->pageView.zoomBy(1.25f));
            LinearLayout navigation=new LinearLayout(this);navigation.setGravity(Gravity.CENTER_VERTICAL);navigation.setBackgroundColor(BAR);navigation.setPadding(dp(12),0,dp(12),0);
            previous=button(BACK,"Página anterior","pdf-previous");next=button(NEXT,"Próxima página","pdf-next");
            position=new TextView(this);position.setTag("pdf-position");position.setGravity(Gravity.CENTER);position.setTextColor(INK);position.setTextSize(14);position.setPadding(dp(8),0,dp(8),0);position.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            navigation.addView(previous,new LinearLayout.LayoutParams(dp(48),dp(48)));navigation.addView(position,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));navigation.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));divider(root);root.addView(navigation);
            previous.setOnClickListener(view->{if(pageIndex>0){pageIndex--;renderPage();}});
            next.setOnClickListener(view->{if(pageIndex+1<renderer.getPageCount()){pageIndex++;renderPage();}});
            setContentView(root);renderPage();
            if(state!=null)pageView.zoomBy(state.getFloat("zoom",1));
        }catch(Exception error){
            new AlertDialog.Builder(this).setMessage("Não foi possível abrir este PDF local.").setPositiveButton("Voltar",(dialog,which)->finish()).setOnCancelListener(dialog->finish()).show();
        }
    }
    private void printPdf(){
        try {
            android.print.PrintManager manager=(android.print.PrintManager)getSystemService(PRINT_SERVICE);
            if(manager==null)throw new IllegalStateException("Impressão indisponível");
            String path=getIntent().getStringExtra("path"),title=getIntent().getStringExtra("title");
            manager.print(title==null||title.trim().isEmpty()?"Família Servita":title,
                new LocalPdfPrintAdapter(this,path,renderer.getPageCount()),
                new android.print.PrintAttributes.Builder().setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).build());
        }catch(Exception error){
            new AlertDialog.Builder(this).setMessage("Não foi possível abrir a impressão. Verifique o serviço de impressão nas configurações do Android.").setPositiveButton("OK",null).show();
        }
    }
    private void renderPage(){
        try(PdfRenderer.Page page=renderer.openPage(pageIndex)){
            int width=Math.min(1800,Math.max(800,Math.round(getResources().getDisplayMetrics().widthPixels*1.5f)));
            int height=Math.max(1,Math.round(width*(float)page.getHeight()/page.getWidth()));
            if(height>3000){width=Math.max(1,width*3000/height);height=3000;}
            Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);
            page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pageView.show(bitmap);position.setText("Página "+(pageIndex+1)+" de "+renderer.getPageCount());
            previous.setEnabled(pageIndex>0);next.setEnabled(pageIndex+1<renderer.getPageCount());
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("page",pageIndex);if(pageView!=null)state.putFloat("zoom",pageView.zoom);}
    @Override protected void onDestroy(){if(pageView!=null)pageView.release();if(renderer!=null)renderer.close();super.onDestroy();}
    private final class PageView extends View {
        private Bitmap bitmap;
        private float zoom=1,panX,panY,lastX,lastY;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final ScaleGestureDetector scale;
        PageView(){super(LocalPdfActivity.this);setBackgroundColor(0xffe5e5e5);scale=new ScaleGestureDetector(getContext(),new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector detector){zoomBy(detector.getScaleFactor());return true;}
        });}
        void show(Bitmap value){Bitmap old=bitmap;bitmap=value;zoom=1;panX=panY=0;describe();invalidate();if(old!=null)old.recycle();}
        void zoomBy(float factor){zoom=Math.max(1,Math.min(4,zoom*factor));if(zoom==1)panX=panY=0;describe();invalidate();}
        void describe(){setContentDescription("PDF. Página "+(pageIndex+1)+". Ampliação "+Math.round(zoom*100)+"%. Use os botões para ampliar ou mudar de página.");}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);if(bitmap==null)return;
            float fit=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());
            float width=bitmap.getWidth()*fit*zoom,height=bitmap.getHeight()*fit*zoom;
            panX=Math.max(-Math.max(0,(width-getWidth())/2),Math.min(panX,Math.max(0,(width-getWidth())/2)));
            panY=Math.max(-Math.max(0,(height-getHeight())/2),Math.min(panY,Math.max(0,(height-getHeight())/2)));
            float left=(getWidth()-width)/2+panX,top=(getHeight()-height)/2+panY;
            canvas.drawBitmap(bitmap,null,new RectF(left,top,left+width,top+height),paint);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            scale.onTouchEvent(event);
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){lastX=event.getX();lastY=event.getY();return true;}
            if(event.getActionMasked()==MotionEvent.ACTION_MOVE){if(!scale.isInProgress()&&event.getPointerCount()==1&&zoom>1){panX+=event.getX()-lastX;panY+=event.getY()-lastY;invalidate();}lastX=event.getX();lastY=event.getY();return true;}
            if(event.getActionMasked()==MotionEvent.ACTION_UP){performClick();return true;}
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        void release(){if(bitmap!=null){bitmap.recycle();bitmap=null;}}
    }
}
