package edu.taller.ascii;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final int HOME=0, ENCODE=1, DECODE=2, SETTINGS=3, EXPORT=41;
    private static final int WHITE=0xffFFFFFF, PURPLE=0xff6520CE, BLUE=0xff2838B3;
    private static final String[] SEPARATORS={" ", ", ", "; "};
    private final String[] drafts={"","",""}, results={null,null,null};
    private SharedPreferences prefs;
    private int screen=HOME, bg, panel, ink, muted, tint, accent, line;
    private boolean dark, en;
    private float scale;
    private String pendingCsv;
    private LinearLayout root, body;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("preferences", MODE_PRIVATE);
        if(state!=null) {
            screen=state.getInt("screen");
            String[] a=state.getStringArray("drafts"), b=state.getStringArray("results");
            if(a!=null) System.arraycopy(a,0,drafts,0,3);
            if(b!=null) System.arraycopy(b,0,results,0,3);
            pendingCsv=state.getString("csv");
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("screen",screen);
        state.putStringArray("drafts",drafts);
        state.putStringArray("results",results);
        state.putString("csv",pendingCsv);
    }
    @Override public void onBackPressed() {
        if(screen==HOME) super.onBackPressed();
        else open(HOME);
    }

    private String tr(String es,String eng){return en?eng:es;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void colors() {
        dark=prefs.getBoolean("dark",false);
        en=prefs.getBoolean("english",false);
        scale=new float[]{.90f,1f,1.16f}[prefs.getInt("size",1)];
        bg=dark?0xff151127:0xffF4F1FC;
        panel=dark?0xff241C3B:WHITE;
        ink=dark?0xffF8F3FF:0xff211344;
        muted=dark?0xffD6C9EB:0xff564970;
        tint=dark?0xff352551:0xffECE4FD;
        accent=dark?0xffE0CCFF:0xff5420AF;
        line=dark?0xff594477:0xffDCD1EF;
    }

    private GradientDrawable surface(int color,int radius) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable outline(int color,int radius) {
        GradientDrawable d=surface(color,radius); d.setStroke(dp(1),line); return d;
    }
    private GradientDrawable gradient(int a,int b,int radius) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});
        d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout column(){LinearLayout v=new LinearLayout(this); v.setOrientation(1); return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this); v.setGravity(Gravity.CENTER_VERTICAL); return v;}
    private void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    private void add(LinearLayout p,View v,int gap) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(gap); p.addView(v,lp);
    }
    private void weighted(LinearLayout p,View v,int gap) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
        lp.leftMargin=dp(gap); p.addView(v,lp);
    }
    private TextView label(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size*scale); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); return t;
    }
    private void tap(View v,Drawable shape,Runnable action) {
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x38A672FF),shape,null));
        v.setClickable(true); v.setFocusable(true); v.setOnClickListener(a->action.run());
    }
    private TextView action(String title,boolean green,Runnable run) {
        TextView t=label(title,15,WHITE,true); t.setGravity(Gravity.CENTER); pad(t,8,11);
        t.setMinHeight(dp(48)); tap(t,green?surface(0xff00785F,14):gradient(0xff7931E8,0xff4D16A7,14),run);
        t.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){
                super.onInitializeAccessibilityNodeInfo(host,info); info.setClassName(Button.class.getName());
            }
        });
        return t;
    }

    private void open(int id) {
        View focus=getCurrentFocus();
        if(focus!=null) getSystemService(InputMethodManager.class).hideSoftInputFromWindow(focus.getWindowToken(),0);
        screen=id; render();
    }
    private void render() {
        colors();
        getWindow().setStatusBarColor(0xff5420B8);
        getWindow().setNavigationBarColor(0xff261859);
        root=column(); root.setBackgroundColor(Build.VERSION.SDK_INT>=35?0xff5420B8:bg);
        root.setFocusableInTouchMode(true);
        root.setOnApplyWindowInsetsListener((v,ins)->{
            if(Build.VERSION.SDK_INT>=35) {
                android.graphics.Insets bars=ins.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                int ime=ins.getInsets(WindowInsets.Type.ime()).bottom;
                root.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime));

            }
            return ins;
        });
        setContentView(root);
        header();
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout frame=column(); scroll.addView(frame,new ScrollView.LayoutParams(-1,-2));
        body=column(); pad(body,18,18); frame.addView(body,new LinearLayout.LayoutParams(-1,-2));
        if(screen==HOME) home();
        else if(screen==SETTINGS) settings();
        else translator(screen==ENCODE);
        if(screen==HOME||screen==SETTINGS) {
            View spacer=new View(this);
            frame.addView(spacer,new LinearLayout.LayoutParams(-1,0,1));
            frame.addView(new Waves(),new LinearLayout.LayoutParams(-1,dp(60)));
        }
        root.requestFocus();
    }

    private void header() {
        LinearLayout top=column(); pad(top,18,14);
        top.setBackground(gradient(PURPLE,BLUE,0));
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        if(screen==HOME) {
            TextView badge=label("A  /  65",19,WHITE,true);
            badge.setGravity(Gravity.CENTER); pad(badge,14,6); badge.setBackground(surface(0x33FFFFFF,12));
            top.setGravity(Gravity.CENTER_HORIZONTAL); top.addView(badge);
            TextView title=label(tr("Traductor ASCII","ASCII Translator"),29,WHITE,true);
            title.setGravity(Gravity.CENTER); add(top,title,6);
            TextView intro=label(tr("Convierte texto y códigos ASCII decimales","Convert text and decimal ASCII codes"),15,0xffF1E9FF,false);
            intro.setGravity(Gravity.CENTER); add(top,intro,4);
        } else {
            LinearLayout bar=row(); top.addView(bar);
            TextView back=label("‹",29,WHITE,true); back.setGravity(Gravity.CENTER);
            back.setContentDescription(tr("Volver al menú","Back to menu")); back.setMinHeight(dp(48));
            tap(back,surface(0x30FFFFFF,12),()->open(HOME)); bar.addView(back,new LinearLayout.LayoutParams(dp(48),-2));
            TextView title=label(screen==ENCODE?tr("Texto a ASCII","Text to ASCII"):
                screen==DECODE?tr("ASCII a Texto","ASCII to Text"):tr("Ajustes","Settings"),23,WHITE,true);
            weighted(bar,title,14);
        }
    }

    private void home() {
        add(body,label(tr("Elige una herramienta","Choose a tool"),18,ink,true),0);
        LinearLayout pair=row(); pair.setGravity(Gravity.TOP); add(body,pair,12);
        weighted(pair,tile("A → 65",tr("Texto a ASCII","Text to ASCII"),()->open(ENCODE)),0);
        weighted(pair,tile("65 → A",tr("ASCII a Texto","ASCII to Text"),()->open(DECODE)),12);
        add(body,tile("⚙",tr("Ajustes","Settings"),()->open(SETTINGS)),12);
        TextView hint=label(tr("ASCII estándar  ·  0 a 127","Standard ASCII  ·  0 to 127"),14,muted,false);
        hint.setGravity(Gravity.CENTER); add(body,hint,22);
    }
    private LinearLayout tile(String icon,String title,Runnable run) {
        LinearLayout tile=column(); tile.setGravity(Gravity.CENTER); pad(tile,8,15);
        tile.setMinimumHeight(dp(102)); tile.setElevation(dp(2));
        add(tile,label(icon,25,accent,true),0);
        TextView caption=label(title,15,ink,true); caption.setGravity(Gravity.CENTER); add(tile,caption,6);
        tile.setContentDescription(title); tap(tile,outline(panel,18),run); return tile;
    }

    private void translator(boolean encoding) {
        final int mode=screen;
        TextView tip=label(encoding?tr("ⓘ  Convierte cada carácter en un número decimal.","ⓘ  Convert each character to a decimal number."):
            tr("ⓘ  Separa los números con espacios, comas o punto y coma.","ⓘ  Separate numbers with spaces, commas or semicolons."),14,accent,false);
        tip.setBackground(surface(tint,14)); pad(tip,13,10); add(body,tip,0);
        TextView inputLabel=label(encoding?tr("Texto o palabra","Text or word"):tr("Código ASCII","ASCII codes"),16,ink,true);
        add(body,inputLabel,14);
        EditText field=new EditText(this); field.setId(View.generateViewId()); inputLabel.setLabelFor(field.getId());
        field.setSaveEnabled(false); field.setTextSize(17*scale); field.setTextColor(ink);
        field.setHintTextColor(muted); field.setBackground(outline(panel,13)); pad(field,12,10);
        field.setGravity(Gravity.TOP); field.setMinLines(2); field.setMaxLines(4);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|
            android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setHint(encoding?"HOLA":"72 79 76 65"); field.setText(drafts[mode]); add(body,field,6);

        if(encoding) {
            LinearLayout tools=row(); add(body,tools,9);
            weighted(tools,action(tr("Elegir carácter","Pick character"),false,()->pick(field)),0);
            weighted(tools,action(tr("Arte ASCII","ASCII art"),false,()->art(field)),8);
        }
        TextView error=label("",14,dark?0xffFFB5C5:0xffA81842,true); error.setVisibility(View.GONE);
        add(body,error,6);
        LinearLayout box=column(); box.setBackground(outline(panel,18)); pad(box,12,11);
        add(box,label(encoding?tr("‹/›  Código ASCII","‹/›  ASCII codes"):tr("Aa  Texto decodificado","Aa  Decoded text"),16,ink,true),0);
        TextView output=label("",22,ink,true); output.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
        output.setTextIsSelectable(true); pad(output,12,12); output.setMinHeight(dp(56));
        output.setBackground(surface(tint,12)); add(box,output,8);

        LinearLayout actions=row();
        TextView copy=action(tr("Copiar","Copy"),true,()->copy(results[mode]));
        TextView clear=action(tr("Limpiar","Clear"),false,()->{
            field.setText(""); results[mode]=null; error.setVisibility(View.GONE);
        });
        TextView export=action("CSV",false,()->export(mode));
        export.setContentDescription(tr("Exportar CSV","Export CSV"));
        weighted(actions,copy,0); weighted(actions,clear,8); weighted(actions,export,8);

        Runnable refresh=()->{
            String value=results[mode];
            output.setText(value==null?tr("Aquí verás el resultado","Your result appears here"):Ascii.display(value));
            output.setTextSize((value==null?15:22)*scale);
            output.setTextColor(value==null?muted:ink);
            copy.setEnabled(value!=null); copy.setAlpha(value==null?.55f:1f);
            export.setEnabled(value!=null); export.setAlpha(value==null?.55f:1f);
        };
        field.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                drafts[mode]=s.toString(); results[mode]=null; error.setVisibility(View.GONE); refresh.run();
            }
            public void afterTextChanged(Editable s){}
        });
        add(body,action(encoding?tr("▶  Traducir","▶  Translate"):tr("▶  Decodificar","▶  Decode"),false,()->{
            try {
                results[mode]=encoding?Ascii.encode(drafts[mode],SEPARATORS[prefs.getInt("separator",0)]):Ascii.decode(drafts[mode]);
                error.setVisibility(View.GONE);
                getSystemService(InputMethodManager.class).hideSoftInputFromWindow(field.getWindowToken(),0);
                field.clearFocus();
            } catch(IllegalArgumentException ex) {
                results[mode]=null; error.setText(message(ex.getMessage())); error.setVisibility(View.VISIBLE);
                error.announceForAccessibility(error.getText());
            }
            refresh.run();
        }),10);
        add(body,box,14); add(body,actions,10);
        refresh.run();
    }

    private String message(String key) {
        switch(key) {
            case "emptyText": return tr("Escribe texto o elige un carácter.","Enter text or pick a character.");
            case "emptyCodes": return tr("Escribe al menos un código ASCII.","Enter at least one ASCII code.");
            case "nonAscii": return tr("Usa ASCII estándar: no admite tildes, ñ ni emojis.","Use standard ASCII: accents and emoji are outside its range.");
            case "range": return tr("Cada código debe estar entre 0 y 127.","Each code must be between 0 and 127.");
            default: return tr("Escribe números decimales sin signos, separados correctamente.","Enter unsigned decimal numbers with valid separators.");
        }
    }

    private AlertDialog.Builder dialog(String title) {
        return new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:
            android.R.style.Theme_Material_Light_Dialog_Alert).setTitle(title);
    }
    private void pick(EditText field) {
        String[] options=new String[95];
        for(int i=0;i<95;i++) options[i]=(i==0?tr("Espacio","Space"):String.valueOf((char)(i+32)))+"  ·  "+(i+32);
        dialog(tr("Selecciona un carácter","Select a character")).setItems(options,(d,i)->{
            int a=Math.max(0,field.getSelectionStart()), b=Math.max(0,field.getSelectionEnd());
            int start=field.hasFocus()?Math.min(a,b):field.length();
            int end=field.hasFocus()?Math.max(a,b):field.length();
            field.getText().replace(start,end,String.valueOf((char)(32+i)));
            field.setSelection(start+1);
        }).setNegativeButton(tr("Cancelar","Cancel"),null).show();
    }
    private void art(EditText field) {
        String s=field.getText().toString().trim();
        if(s.isEmpty()||s.charAt(0)<33||s.charAt(0)>126) {
            toast(tr("Escribe o elige primero un carácter ASCII visible.","Enter or pick a visible ASCII character first.")); return;
        }
        final char glyph=s.charAt(0);
        String[] marks={"#", "*", "+"};
        dialog(tr("Elige el borde de la figura","Choose the border character")).setItems(marks,(d,i)->{
            String figure=Ascii.art(glyph,marks[i].charAt(0));
            TextView view=label(figure,22,ink,true); pad(view,18,12);
            view.setTypeface(Typeface.MONOSPACE); view.setGravity(Gravity.CENTER); view.setTextIsSelectable(true);
            dialog(tr("Figura ASCII · ","ASCII figure · ")+glyph+" = "+(int)glyph).setView(view)
                .setPositiveButton(tr("Copiar","Copy"),(a,b)->copy(figure))
                .setNegativeButton(tr("Cerrar","Close"),null).show();
        }).setNegativeButton(tr("Cancelar","Cancel"),null).show();
    }
    private void copy(String value) {
        if(value==null)return;
        getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("ASCII",value));
        if(Build.VERSION.SDK_INT<33) toast(tr("Copiado al portapapeles.","Copied to clipboard."));
    }
    private void export(int mode) {
        if(results[mode]==null)return;
        pendingCsv=Ascii.csv(mode==ENCODE?"texto_a_ascii":"ascii_a_texto",drafts[mode],results[mode]);
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/csv").putExtra(Intent.EXTRA_TITLE,"resultado-ascii.csv");
        try { startActivityForResult(intent,EXPORT); }
        catch(ActivityNotFoundException ex){pendingCsv=null;toast(tr("No hay selector de archivos disponible.","No file picker is available."));}
    }
    @Override protected void onActivityResult(int code,int status,Intent data) {
        super.onActivityResult(code,status,data);
        if(code!=EXPORT)return;
        String csv=pendingCsv; pendingCsv=null;
        if(status!=RESULT_OK||data==null||data.getData()==null||csv==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")) {
            if(out==null) throw new IOException();
            out.write(csv.getBytes(StandardCharsets.UTF_8)); out.flush();
        } catch(IOException|SecurityException ex) {
            toast(tr("No se pudo guardar el archivo.","Unable to save the file.")); return;
        }
        toast(tr("CSV guardado.","CSV saved."));
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}

    private void settings() {
        group(tr("Tema","Theme"),new String[]{tr("Claro","Light"),tr("Oscuro","Dark")},dark?1:0,
            i->prefs.edit().putBoolean("dark",i==1).apply());
        group(tr("Separador de códigos","Code separator"),
            new String[]{tr("Espacio","Space"),tr("Coma","Comma"),tr("Punto y coma","Semicolon")},prefs.getInt("separator",0),
            i->{prefs.edit().putInt("separator",i).apply(); results[ENCODE]=null;});
        group(tr("Tamaño de letra","Font size"),new String[]{"A−","A","A+"},prefs.getInt("size",1),
            i->prefs.edit().putInt("size",i).apply());
        group(tr("Idioma","Language"),new String[]{"Español","English"},en?1:0,
            i->prefs.edit().putBoolean("english",i==1).apply());
        TextView about=label(tr("ⓘ   Acerca de   ›","ⓘ   About   ›"),16,accent,true); pad(about,14,14);
        tap(about,outline(panel,15),()->dialog(tr("Acerca de Traductor ASCII","About ASCII Translator"))
            .setMessage(tr("Versión 1.0 · Taller 3\nConvierte ASCII decimal del 0 al 127. La aplicación funciona sin conexión.",
                "Version 1.0 · Workshop 3\nConverts decimal ASCII from 0 to 127. The app works offline."))
            .setPositiveButton(tr("Cerrar","Close"),null).show());
        add(body,about,12);
    }
    private interface Choice {void choose(int i);}
    private void group(String title,String[] values,int selected,Choice change) {
        LinearLayout card=column(); card.setBackground(outline(panel,15)); pad(card,12,9);
        add(card,label(title,16,ink,true),0);
        if(values.length==3 && !values[0].equals("A−")) {
            TextView picker=label(values[selected]+"   ▾",14,accent,true); pad(picker,10,10); picker.setMinHeight(dp(48));
            tap(picker,surface(tint,10),()->dialog(title).setSingleChoiceItems(values,selected,(d,i)->{
                d.dismiss(); change.choose(i); render();
            }).setNegativeButton(tr("Cancelar","Cancel"),null).show());
            add(card,picker,5);
        } else {
            LinearLayout group=row(); add(card,group,5);
            for(int i=0;i<values.length;i++) {
                final int index=i;
                TextView option=label(values[i],14,i==selected?WHITE:accent,true);
                option.setGravity(Gravity.CENTER); option.setMinHeight(dp(48)); pad(option,4,7);
                option.setSelected(i==selected);
                tap(option,surface(i==selected?PURPLE:tint,10),()->{change.choose(index); render();});
                weighted(group,option,i==0?0:6);
            }
        }
        add(body,card,body.getChildCount()==0?0:10);
    }

    private final class Waves extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Waves(){super(MainActivity.this);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas) {
            float w=getWidth(), h=getHeight();
            Path p=new Path();
            p.moveTo(0,h*.20f); p.cubicTo(w*.24f,-h*.35f,w*.38f,h*.94f,w*.65f,h*.43f);
            p.cubicTo(w*.79f,h*.18f,w*.91f,-h*.19f,w,h*.16f);
            p.lineTo(w,h); p.lineTo(0,h); p.close();
            paint.setShader(new LinearGradient(0,0,w,h,0xffB37AFF,0xff6428DD,Shader.TileMode.CLAMP)); canvas.drawPath(p,paint);
            p.reset(); p.moveTo(0,h*.56f); p.cubicTo(w*.23f,-h*.06f,w*.42f,h*1.12f,w*.70f,h*.66f);
            p.cubicTo(w*.83f,h*.45f,w*.93f,h*.15f,w,h*.36f);
            p.lineTo(w,h); p.lineTo(0,h); p.close();
            paint.setShader(new LinearGradient(0,0,w,h,0xff3C49D9,0xff591BB9,Shader.TileMode.CLAMP)); canvas.drawPath(p,paint);
        }
    }
}


