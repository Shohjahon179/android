package uz.vagoncontrol

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Navy = Color(0xFF07182D)
private val Navy2 = Color(0xFF102D4D)
private val Gold = Color(0xFFD8B45A)
private val Cream = Color(0xFFF5F3EC)
private val Muted = Color(0xFFB8C6D6)
private val Red = Color(0xFFFF8A80)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); val store=Store(this); setContent { VagonApp(store) } }
}

@Composable private fun VagonApp(store: Store) {
    var screen by rememberSaveable { mutableStateOf("auth") }
    var login by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf("") }
    var preview by rememberSaveable { mutableStateOf(false) }
    MaterialTheme(colorScheme=darkColorScheme(primary=Gold,secondary=Gold,background=Navy,surface=Navy2,onPrimary=Navy)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy,Navy2)))) {
            BackHandler(enabled=screen!="auth") { screen=when(screen){"preview"->"table";"editor"->"table";else->"home"} }
            AnimatedContent(screen, transitionSpec={ (fadeIn(tween(260))+slideInHorizontally(tween(260)){it/8}) togetherWith fadeOut(tween(180)) }, label="screen") { s ->
                when(s){
                    "auth" -> AuthScreen(store){login=it;screen="home"}
                    "home" -> HomeScreen(store,login,{screen="editor";editingId=""},{screen="table"},{screen="profile"},{screen="admin"},{login="";screen="auth"})
                    "editor" -> RecordEditor(store,login,editingId,{screen="table"})
                    "table" -> TableScreen(store,login,{screen="home"},{id->editingId=id;screen="editor"},{screen="preview"})
                    "preview" -> PrintPreview(store,{screen="table"})
                    "profile" -> ProfileScreen(store,login,{screen="home"})
                    "admin" -> AdminScreen(store,{screen="home"})
                }
            }
        }
    }
}

@Composable private fun AuthScreen(store: Store,onSuccess:(String)->Unit){
    val ctx = LocalContext.current
    var register by remember{mutableStateOf(false)}; var login by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var show by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    val pulse by rememberInfiniteTransition(label="train").animateFloat(1f,1.07f,infiniteRepeatable(tween(1400),RepeatMode.Reverse),label="pulse")
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Text("🚆",fontSize=66.sp,modifier=Modifier.scale(pulse));Text("VAGON BOSHQARUVI",color=Gold,fontSize=25.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp);Text("Temiryo‘l vagonlari nazorati",color=Muted,fontSize=13.sp)
        Spacer(Modifier.height(26.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color(0x252B4966))){Column(Modifier.padding(20.dp)){
            Text(if(register)"Ro‘yxatdan o‘tish" else "Tizimga kirish",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color.White);Spacer(Modifier.height(10.dp))
            if(register){InputField("F.I.Sh.",name,{name=it});InputField("Telefon",phone,{phone=it},keyboard=KeyboardType.Phone)}
            InputField("Login",login,{login=it},keyboard=KeyboardType.Text)
            InputField("Parol",pass,{pass=it},password=true,showPassword=show,onTogglePassword={show=!show})
            if(error.isNotBlank())Text(error,color=Red,fontSize=13.sp)
            Spacer(Modifier.height(12.dp));GoldButton(if(register)"Ro‘yxatdan o‘tish" else "Kirish",Modifier.fillMaxWidth()){
                error=if(register){store.register(login,pass,name,phone)?:""}else if(store.login(login,pass))"" else "Login yoki parol noto‘g‘ri"
                if(error.isEmpty()){if(register)Toast.makeText(ctx,"Ro‘yxatdan o‘tildi. Endi kiring.",Toast.LENGTH_SHORT).show() else onSuccess(login.trim())}
            }
            TextButton(onClick={register=!register;error=""}){Text(if(register)"Akkauntim bor — kirish" else "Yangi xodim sifatida ro‘yxatdan o‘tish",color=Muted)}
            Text("Admin demo kirish: admin / VagonAdmin2026!",fontSize=10.sp,color=Muted)
        }}
    }
}

@Composable private fun InputField(label:String,value:String,onChange:(String)->Unit,keyboard:KeyboardType=KeyboardType.Text,password:Boolean=false,showPassword:Boolean=false,onTogglePassword:()->Unit={}){
    Column(Modifier.fillMaxWidth().padding(vertical=5.dp)){Text(label,color=Gold,fontSize=12.sp,fontWeight=FontWeight.SemiBold);OutlinedTextField(value,onChange,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(keyboardType=if(password)KeyboardType.Password else keyboard),visualTransformation=if(password&&!showPassword)PasswordVisualTransformation() else VisualTransformation.None,trailingIcon=if(password)({TextButton(onClick=onTogglePassword){Text(if(showPassword)"YASHIR" else "KO‘RISH",color=Gold,fontSize=10.sp)}})else null,colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=Gold,unfocusedBorderColor=Color(0x668FA6C0),cursorColor=Gold))}
}
@Composable private fun GoldButton(text:String,modifier:Modifier=Modifier,onClick:()->Unit){Button(onClick=onClick,modifier=modifier.heightIn(min=50.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Navy)){Text(text,fontWeight=FontWeight.Bold)}}
@Composable private fun Panel(title:String,subtitle:String="",content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp)){Text(title,color=Gold,fontSize=24.sp,fontWeight=FontWeight.Bold);if(subtitle.isNotBlank())Text(subtitle,color=Muted,fontSize=12.sp);Spacer(Modifier.height(14.dp));content()}}
@Composable private fun Header(title:String,onBack:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack){Text("← Orqaga",color=Muted)};Spacer(Modifier.weight(1f));Text(title,color=Gold,fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp))}}

@Composable private fun HomeScreen(store:Store,login:String,onAdd:()->Unit,onTable:()->Unit,onProfile:()->Unit,onAdmin:()->Unit,onLogout:()->Unit){val user=store.user(login);val role=user?.role?:"XODIM";val count=store.records().size
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(18.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("VAGON BOSHQARUVI",color=Gold,fontSize=20.sp,fontWeight=FontWeight.Black);Text("Temiryo‘l boshqaruv tizimi",color=Muted,fontSize=11.sp)};Text("🚆",fontSize=35.sp)}
        Spacer(Modifier.height(22.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color(0x243F5C7D))){Column(Modifier.padding(18.dp)){Text("Xush kelibsiz",color=Muted);Text(user?.fullName?.ifBlank{login}?:login,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold);Text(when(role){"ADMIN"->"ADMINISTRATOR";"RAHBAR"->"RAHBAR";else->"XODIM"},color=Gold,fontSize=12.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));Text("Jami vagon yozuvlari: $count",color=Color.White)}}
        Spacer(Modifier.height(18.dp));if(role!="XODIM")MenuTile("📋","Umumiy jadval","Barcha vagonlar va vaqtlar",onTable)
        if(role=="XODIM"){MenuTile("➕","Yangi vagon kiritish","Poyezd va vagon ma’lumotlari",onAdd);MenuTile("📋","Jadvalni ko‘rish","O‘zingiz kiritgan ma’lumotlar",onTable)}
        if(role=="RAHBAR"||role=="ADMIN")MenuTile("➕","Yangi vagon kiritish","Yangi yozuv qo‘shish",onAdd)
        MenuTile("👤","Profil","Shaxsiy ma’lumotlarni tahrirlash",onProfile)
        if(role=="ADMIN")MenuTile("⚙️","Admin panel","Foydalanuvchi va rollarni boshqarish",onAdmin)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick=onTable,modifier=Modifier.weight(1f),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold)){Text("📋 Jadval")}
            OutlinedButton(onClick=onProfile,modifier=Modifier.weight(1f),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold)){Text("👤 Profil")}
        }
        TextButton(onClick=onLogout,modifier=Modifier.fillMaxWidth()){Text("Chiqish",color=Red)}
    }
}
@Composable private fun MenuTile(icon:String,title:String,desc:String,onClick:()->Unit){Card(Modifier.fillMaxWidth().padding(vertical=6.dp),onClick=onClick,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0x223F5C7D))){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=28.sp);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,color=Color.White,fontWeight=FontWeight.Bold);Text(desc,color=Muted,fontSize=11.sp)};Text("›",color=Gold,fontSize=26.sp)}}}

@Composable private fun RecordEditor(store:Store,login:String,id:String,onDone:()->Unit){val ctx=LocalContext.current;val old=remember(id){store.records().firstOrNull{it.id==id}};var train by remember{mutableStateOf(old?.train?:"")};var wagons by remember{mutableStateOf(old?.wagon?:"")};var date by remember{mutableStateOf(old?.date?:today())};var arrival by remember{mutableStateOf(old?.arrival?:nowTime())};var given by remember{mutableStateOf(old?.givenBy?:"")};var received by remember{mutableStateOf(old?.receivedAt?:"")};var departBy by remember{mutableStateOf(old?.departBy?:"")};var departed by remember{mutableStateOf(old?.departedAt?:"")};var note by remember{mutableStateOf(old?.note?:"")};var error by remember{mutableStateOf("")}
    Panel(if(id.isBlank())"Yangi vagon ma’lumoti" else "Ma’lumotni tahrirlash","Vagon raqamlarini vergul bilan ajratib yozish mumkin"){
        InputField("Poyezd raqami",train,{train=it},keyboard=KeyboardType.Number);InputField("Vagon raqamlari (masalan: 625813,657383)",wagons,{wagons=it},keyboard=KeyboardType.Number)
        DateTimeField("Sana",date,{date=it},ctx,true);DateTimeField("Kirib kelgan vaqti",arrival,{arrival=it},ctx,false)
        DateTimeField("Berilgunga qadar",given,{given=it},ctx,false,allowBlank=true)
        DateTimeField("Olingan vaqti",received,{received=it},ctx,false,allowBlank=true)
        DateTimeField("Jo‘nagunga qadar",departBy,{departBy=it},ctx,false,allowBlank=true)
        DateTimeField("Haqiqiy jo‘nagan vaqti",departed,{departed=it},ctx,false,allowBlank=true)
        InputField("Izoh",note,{note=it})
        if(error.isNotBlank())Text(error,color=Red)
        GoldButton("Saqlash",Modifier.fillMaxWidth()){
            val nums=wagons.split(',', ';',' ','\n').map{it.trim()}.filter{it.isNotBlank()}.distinct()
            if(train.isBlank()||nums.isEmpty())error="Poyezd va kamida bitta vagon raqamini kiriting"
            else if(nums.any{it.any{c->!c.isDigit()}||it.length<4})error="Vagon raqamlarini tekshiring"
            else {val list=nums.mapIndexed{index,w->VagonRecord(if(index==0&&id.isNotBlank())id else UUID.randomUUID().toString(),train.trim(),w,date,arrival,given,received,departBy,departed,note,login)};store.saveMany(list);Toast.makeText(ctx,"${list.size} ta vagon saqlandi",Toast.LENGTH_SHORT).show();onDone()}
        }
        if(id.isNotBlank())TextButton(onClick={store.deleteRecord(id);onDone()}){Text("Yozuvni o‘chirish",color=Red)}
    }
}

@Composable private fun DateTimeField(label:String,value:String,onChange:(String)->Unit,ctx:Context,date:Boolean,allowBlank:Boolean=false){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){InputField(label,value,onChange,keyboard=KeyboardType.Text)};TextButton(onClick={if(date)pickDate(ctx,onChange)else pickTime(ctx,onChange)}){Text(if(date)"📅" else "🕒",fontSize=21.sp)}}}
private fun pickDate(ctx:Context,set:(String)->Unit){val c=Calendar.getInstance();DatePickerDialog(ctx,{_,y,m,d->set("%02d.%02d.%04d".format(d,m+1,y))},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()}
private fun pickTime(ctx:Context,set:(String)->Unit){val c=Calendar.getInstance();TimePickerDialog(ctx,{_,h,m->set("%02d:%02d".format(h,m))},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show()}
private fun today()=SimpleDateFormat("dd.MM.yyyy",Locale.US).format(Date())
private fun nowTime()=SimpleDateFormat("HH:mm",Locale.US).format(Date())
private fun millis(date:String,time:String):Long?=try{SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.US).apply{isLenient=false}.parse("$date $time")?.time}catch(_:Exception){null}
private fun remaining(date:String,time:String):String{val target=millis(date,time)?:return "—";val d=target-System.currentTimeMillis();if(d<=0)return "Muddati o‘tgan";val sec=d/1000;return "${sec/86400} kun ${(sec%86400)/3600} soat ${(sec%3600)/60} daq"}

@Composable private fun TableScreen(store:Store,login:String,onBack:()->Unit,onEdit:(String)->Unit,onPreview:()->Unit){var tick by remember{mutableLongStateOf(0L)};LaunchedEffect(Unit){while(true){delay(1000);tick++}};var filter by remember{mutableStateOf("")};val user=store.user(login);val rows=remember(tick,filter){store.records().filter{filter.isBlank()||it.train.contains(filter,true)||it.wagon.contains(filter,true)}}
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(14.dp)){Header("Vagonlar jadvali",onBack);OutlinedTextField(filter,{filter=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Poyezd yoki vagon raqami bo‘yicha qidirish")});Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("Jami: ${rows.size}",color=Gold);Text("Yangilanish: avtomatik",color=Muted,fontSize=11.sp)}
        LazyColumn(Modifier.weight(1f)){items(rows,key={it.id}){r->Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color(0x253A5675))){Column(Modifier.padding(13.dp)){Row{Text("Poyezd ${r.train}",color=Gold,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text("Vagon ${r.wagon}",color=Color.White,fontWeight=FontWeight.Bold)};Text("Sana: ${r.date}  •  Kirish: ${r.arrival}",color=Muted,fontSize=12.sp);Text("Berilgunga qadar: ${if(r.givenBy.isBlank())"—" else remaining(r.date,r.givenBy)}",color=Color.White,fontSize=12.sp);Text("Olingan vaqti: ${r.receivedAt.ifBlank{"—"}}",color=Color.White,fontSize=12.sp);Text("Jo‘nagunga qadar: ${if(r.departBy.isBlank())"—" else remaining(r.date,r.departBy)}",color=Color.White,fontSize=12.sp);Text("Kiritdi: ${r.owner}",color=Muted,fontSize=10.sp);if(user?.role=="ADMIN"||user?.role=="RAHBAR"||r.owner==login)TextButton(onClick={onEdit(r.id)}){Text("✏️ Tahrirlash",color=Gold)}}}}}
        GoldButton("Ochiq betda ko‘rish",Modifier.fillMaxWidth(),onPreview)
    }
}

@Composable private fun PrintPreview(store:Store,onBack:()->Unit){val rows=store.records();Column(Modifier.fillMaxSize().background(Color.White).systemBarsPadding().padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onBack){Text("← Orqaga",color=Color.Black)};Spacer(Modifier.weight(1f));Text("A4 jadval",color=Color.Black)};Text("VAGONLAR HISOBI JADVALI",color=Color.Black,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));Row(Modifier.horizontalScroll(rememberScrollState())){Column{Row(Modifier.background(Color(0xFFE8EDF3))){listOf("№","Poyezd","Vagon raqami","Sana","Kirish","Berilgunga qadar","Olingan","Jo‘nagunga qadar").forEach{Text(it,color=Color.Black,fontWeight=FontWeight.Bold,fontSize=10.sp,modifier=Modifier.width(112.dp).padding(5.dp))}};rows.forEachIndexed{index,r->Row(Modifier.background(if(index%2==0)Color.White else Color(0xFFF6F8FA))){listOf("${index+1}",r.train,r.wagon,r.date,r.arrival,r.givenBy,r.receivedAt,r.departBy).forEach{Text(it.ifBlank{"—"},color=Color.Black,fontSize=10.sp,modifier=Modifier.width(112.dp).padding(5.dp))}}}}}}
}

@Composable private fun ProfileScreen(store:Store,login:String,onBack:()->Unit){val ctx=LocalContext.current;val u=store.user(login);var name by remember{mutableStateOf(u?.fullName?:"")};var phone by remember{mutableStateOf(u?.phone?:"")};Panel("Profil", "Lavozim: ${u?.role?:"XODIM"}"){InputField("F.I.Sh.",name,{name=it});InputField("Telefon",phone,{phone=it},keyboard=KeyboardType.Phone);GoldButton("Profilni saqlash",Modifier.fillMaxWidth()){store.updateProfile(login,name,phone);Toast.makeText(ctx,"Saqlandi",Toast.LENGTH_SHORT).show()};TextButton(onClick=onBack){Text("Orqaga",color=Muted)}}}

@Composable private fun AdminScreen(store:Store,onBack:()->Unit){val ctx=LocalContext.current;var refresh by remember{mutableIntStateOf(0)};var selected by remember{mutableStateOf("")};var newPass by remember{mutableStateOf("")};val users=remember(refresh){store.allUsers()};Panel("Admin panel","Foydalanuvchilar va rollarni boshqarish") {users.forEach{u->Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=Color(0x253A5675))){Column(Modifier.padding(12.dp)){Text(u.fullName.ifBlank{u.login},color=Color.White,fontWeight=FontWeight.Bold);Text("Login: ${u.login}  •  Rol: ${u.role}",color=Muted,fontSize=12.sp);Text("Parol: •••••••• (xavfsiz saqlangan)",color=Muted,fontSize=11.sp);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={store.setRole(u.login,"XODIM");refresh++}){Text("Xodim",color=Gold)};TextButton(onClick={store.setRole(u.login,"RAHBAR");refresh++}){Text("Rahbar",color=Gold)};TextButton(onClick={selected=u.login}){Text("Parolni yangilash",color=Gold)};if(u.login!="admin")TextButton(onClick={store.deleteUser(u.login);refresh++}){Text("O‘chirish",color=Red)}}}}};if(selected.isNotBlank()){Text("${selected} uchun yangi parol",color=Gold);InputField("Yangi parol",newPass,{newPass=it},password=true);GoldButton("Yangilash",Modifier.fillMaxWidth()){if(store.resetPassword(selected,newPass)){selected="";newPass="";Toast.makeText(ctx,"Parol yangilandi",Toast.LENGTH_SHORT).show()}else Toast.makeText(ctx,"Parol kamida 6 belgi bo‘lsin",Toast.LENGTH_SHORT).show()}};Text("Eslatma: parollarni ko‘rsatish o‘rniga faqat yangilash mumkin.",color=Muted,fontSize=11.sp);TextButton(onClick=onBack){Text("Orqaga",color=Muted)}}}
