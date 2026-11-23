package com.adipack.mdm.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File

/**
 * Logo de la empresa en la pantalla del agente. Es el que se sube en el MDM › «Empresa y logo»: llega en la
 * política ("branding_logo": url + sha256), se descarga verificado y se guarda en el equipo, así se ve aunque
 * la tablet no tenga red. Si en el MDM no hay logo, se usa el logo ADIPACK incluido en la app.
 */
object Branding {
    private fun file(ctx: Context) = File(ctx.filesDir, "branding_logo.png")
    private fun shaFile(ctx: Context) = File(ctx.filesDir, "branding_logo.sha256")

    /** Se llama en cada sincronización: sólo descarga si el logo cambió. */
    fun update(ctx: Context, p: JSONObject, api: Api) {
        val b = p.optJSONObject("branding_logo")
        val sha = b?.optString("sha256").orEmpty()
        if (b == null || sha.isEmpty()) {
            if (p.has("version") && !p.optBoolean("release")) { file(ctx).delete(); shaFile(ctx).delete() }
            return
        }
        val have = try { shaFile(ctx).readText().trim() } catch (e: Exception) { "" }
        if (have.equals(sha, ignoreCase = true) && file(ctx).exists()) return
        val tmp = File(ctx.filesDir, "branding_logo.tmp")
        api.download(b.optString("url"), tmp, sha)
        if (BitmapFactory.decodeFile(tmp.absolutePath) == null) { tmp.delete(); return }  // no es una imagen
        if (!tmp.renameTo(file(ctx))) { tmp.copyTo(file(ctx), overwrite = true); tmp.delete() }
        shaFile(ctx).writeText(sha)
        logI("Logo de la empresa actualizado")
    }

    /** Logo descargado (reducido para no gastar memoria) o null para usar el incluido en la app. */
    fun load(ctx: Context, maxPx: Int): Bitmap? {
        val f = file(ctx)
        if (!f.exists()) return null
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, o)
            var sample = 1
            while (o.outWidth / (sample * 2) >= maxPx || o.outHeight / (sample * 2) >= maxPx) sample *= 2
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) { logE("logo", e); null }
    }
}
