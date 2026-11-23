package uz.vagoncontrol

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

 data class VagonRecord(
    val id: String,
    val train: String,
    val wagon: String,
    val date: String,
    val arrival: String,
    val givenBy: String,
    val receivedAt: String,
    val departBy: String,
    val departedAt: String,
    val note: String,
    val owner: String
)

data class AppUser(val login: String, val role: String, val fullName: String, val phone: String)

class Store(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("vagon_boshqaruvi", Context.MODE_PRIVATE)
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun usersJson() = JSONArray(prefs.getString("users", "[]"))
    private fun recordsJson() = JSONArray(prefs.getString("records", "[]"))

    init {
        if (prefs.getBoolean("initialized", false).not()) {
            val a = JSONArray()
            a.put(JSONObject().put("login", "admin").put("pass", hash("VagonAdmin2026!")).put("role", "ADMIN").put("name", "Tizim administratori").put("phone", ""))
            prefs.edit().putString("users", a.toString()).putBoolean("initialized", true).apply()
        }
    }

    fun login(login: String, password: String): Boolean = (0 until usersJson().length()).any {
        val u = usersJson().getJSONObject(it)
        u.optString("login").equals(login.trim(), true) && u.optString("pass") == hash(password)
    }
    fun register(login: String, password: String, fullName: String, phone: String): String? {
        val a = usersJson()
        if (login.trim().length < 3) return "Login kamida 3 ta belgidan iborat bo'lsin"
        if (password.length < 6) return "Parol kamida 6 ta belgidan iborat bo'lsin"
        if ((0 until a.length()).any { a.getJSONObject(it).optString("login").equals(login.trim(), true) }) return "Bu login band"
        a.put(JSONObject().put("login", login.trim()).put("pass", hash(password)).put("role", "XODIM").put("name", fullName).put("phone", phone))
        prefs.edit().putString("users", a.toString()).apply(); return null
    }
    fun user(login: String): AppUser? {
        val a = usersJson()
        for (i in 0 until a.length()) { val u = a.getJSONObject(i); if (u.optString("login").equals(login, true)) return AppUser(u.optString("login"), u.optString("role", "XODIM"), u.optString("name"), u.optString("phone")) }
        return null
    }
    fun allUsers(): List<AppUser> { val a=usersJson(); return (0 until a.length()).map { val u=a.getJSONObject(it); AppUser(u.optString("login"),u.optString("role"),u.optString("name"),u.optString("phone")) } }
    fun updateProfile(login: String, name: String, phone: String) { val a=usersJson(); for(i in 0 until a.length()){val u=a.getJSONObject(i); if(u.optString("login")==login){u.put("name",name);u.put("phone",phone)}}; prefs.edit().putString("users",a.toString()).apply() }
    fun setRole(login: String, role: String) { val a=usersJson(); for(i in 0 until a.length()){val u=a.getJSONObject(i);if(u.optString("login")==login)u.put("role",role)};prefs.edit().putString("users",a.toString()).apply() }
    fun resetPassword(login: String, password: String): Boolean { if(password.length<6)return false; val a=usersJson(); for(i in 0 until a.length()){val u=a.getJSONObject(i);if(u.optString("login")==login){u.put("pass",hash(password));prefs.edit().putString("users",a.toString()).apply();return true}};return false }
    fun deleteUser(login: String) { val a=usersJson(); val b=JSONArray(); for(i in 0 until a.length()){val u=a.getJSONObject(i);if(u.optString("login")!=login && u.optString("login")!="admin")b.put(u)};prefs.edit().putString("users",b.toString()).apply() }

    fun records(): List<VagonRecord> { val a=recordsJson(); return (0 until a.length()).map { val o=a.getJSONObject(it); VagonRecord(o.optString("id"),o.optString("train"),o.optString("wagon"),o.optString("date"),o.optString("arrival"),o.optString("givenBy"),o.optString("receivedAt"),o.optString("departBy"),o.optString("departedAt"),o.optString("note"),o.optString("owner")) } }
    fun saveRecord(r: VagonRecord) { val all=records().toMutableList(); val i=all.indexOfFirst{it.id==r.id}; if(i>=0)all[i]=r else all.add(r); writeRecords(all) }
    fun saveMany(records: List<VagonRecord>) { writeRecords(records().filterNot { old -> records.any { it.id==old.id } } + records) }
    fun deleteRecord(id: String) { writeRecords(records().filterNot{it.id==id}) }
    private fun writeRecords(list: List<VagonRecord>) { val a=JSONArray(); list.forEach { r -> a.put(JSONObject().put("id",r.id).put("train",r.train).put("wagon",r.wagon).put("date",r.date).put("arrival",r.arrival).put("givenBy",r.givenBy).put("receivedAt",r.receivedAt).put("departBy",r.departBy).put("departedAt",r.departedAt).put("note",r.note).put("owner",r.owner)) };prefs.edit().putString("records",a.toString()).apply() }
}
