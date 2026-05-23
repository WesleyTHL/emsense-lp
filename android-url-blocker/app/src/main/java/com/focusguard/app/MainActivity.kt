package com.focusguard.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
    }

    override fun onResume() {
        super.onResume()
        route()
    }

    override fun onPause() {
        super.onPause()
        cancelCountdown()
    }

    private fun route() {
        when {
            !prefs.hasPassword -> renderSetup()
            !sessionUnlocked -> renderLogin()
            else -> renderHome()
        }
    }

    // --- Screens -------------------------------------------------------------

    private fun renderSetup() {
        val (scroll, col) = screen()
        col.addView(heading("Bienvenue"))
        col.addView(body("Choisis un mot de passe maître. Il sera nécessaire pour modifier la liste de blocage ou désactiver la protection. Choisis-le long et difficile à deviner."))

        val pw1 = input("Mot de passe", password = true)
        val pw2 = input("Confirme le mot de passe", password = true)
        col.addView(pw1)
        col.addView(pw2)

        col.addView(primaryButton("Définir le mot de passe") {
            val a = pw1.text.toString()
            val b = pw2.text.toString()
            when {
                a.length < 6 -> toast("Au moins 6 caractères (plus c'est long, mieux c'est).")
                a != b -> toast("Les deux mots de passe ne correspondent pas.")
                else -> {
                    val salt = Security.newSalt()
                    prefs.salt = Security.encodeSalt(salt)
                    prefs.passwordHash = Security.hash(a.toCharArray(), salt)
                    sessionUnlocked = true
                    toast("Mot de passe défini.")
                    route()
                }
            }
        })
        setContentView(scroll)
    }

    private fun renderLogin() {
        val (scroll, col) = screen()
        col.addView(heading("FocusGuard"))
        col.addView(body("Entre ton mot de passe pour accéder aux réglages."))
        val pw = input("Mot de passe", password = true)
        col.addView(pw)
        col.addView(primaryButton("Déverrouiller") {
            if (checkPassword(pw.text.toString())) {
                sessionUnlocked = true
                route()
            } else {
                toast("Mot de passe incorrect.")
            }
        })
        setContentView(scroll)
    }

    private fun renderHome() {
        val (scroll, col) = screen()
        col.addView(heading("FocusGuard"))

        // Status section
        col.addView(sectionTitle("État"))
        col.addView(statusRow(
            "Service d'accessibilité",
            isAccessibilityEnabled(),
            "Activer"
        ) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        col.addView(statusRow(
            "Protection anti-désinstallation",
            isAdminActive(),
            "Activer"
        ) { requestDeviceAdmin() })

        // Blocklist
        col.addView(sectionTitle("URL bloquées"))
        col.addView(body("Utilise * comme joker. Ex : *youtube.com/shorts* bloque tous les Shorts."))

        val patterns = prefs.patterns
        for (p in patterns) {
            col.addView(patternRow(p))
        }

        val addField = input("Nouveau motif, ex : *reddit.com*", password = false)
        col.addView(addField)
        col.addView(primaryButton("Ajouter à la liste") {
            val v = addField.text.toString().trim()
            if (v.isEmpty()) {
                toast("Saisis un motif.")
            } else {
                prefs.patterns = prefs.patterns + v
                toast("Ajouté.")
                route()
            }
        })

        // Shorts toggle
        col.addView(sectionTitle("YouTube Shorts (application)"))
        val shortsOn = prefs.blockYouTubeShorts
        col.addView(body(if (shortsOn) "Les Shorts sont bloqués dans l'app YouTube." else "Les Shorts ne sont PAS bloqués dans l'app."))
        if (shortsOn) {
            col.addView(secondaryButton("Désactiver le blocage des Shorts (nécessite déverrouillage)") {
                startUnlockFlow { prefs.blockYouTubeShorts = false; toast("Shorts non bloqués."); route() }
            })
        } else {
            col.addView(primaryButton("Réactiver le blocage des Shorts") {
                prefs.blockYouTubeShorts = true; toast("Shorts bloqués."); route()
            })
        }

        // Cooldown
        col.addView(sectionTitle("Délai de déverrouillage"))
        col.addView(body("Délai d'attente (minutes) imposé avant de pouvoir désactiver la protection. Plus il est élevé, plus c'est difficile de céder à une impulsion."))
        val cd = input(password = false, hint = "Minutes", number = true)
        cd.setText(prefs.cooldownMinutes.toString())
        col.addView(cd)
        col.addView(secondaryButton("Enregistrer le délai") {
            val n = cd.text.toString().toIntOrNull()
            if (n == null || n < 0) toast("Nombre invalide.") else {
                prefs.cooldownMinutes = n; toast("Délai = $n min.")
            }
        })

        // Danger zone
        col.addView(sectionTitle("Désactiver la protection"))
        col.addView(body("Pour désactiver le service ou désinstaller l'app, déverrouille d'abord ici. Un compte à rebours de ${prefs.cooldownMinutes} min s'appliquera."))
        col.addView(dangerButton("Déverrouiller la protection système") {
            startUnlockFlow {
                prefs.unlockedUntil = System.currentTimeMillis() + 5 * 60 * 1000
                renderUnlocked()
            }
        })

        col.addView(sectionTitle("Sécurité"))
        col.addView(secondaryButton("Changer le mot de passe") { renderChangePassword() })
        col.addView(secondaryButton("Verrouiller maintenant") {
            sessionUnlocked = false
            route()
        })

        setContentView(scroll)
    }

    private fun renderChangePassword() {
        val (scroll, col) = screen()
        col.addView(heading("Changer le mot de passe"))
        val old = input("Mot de passe actuel", password = true)
        val n1 = input("Nouveau mot de passe", password = true)
        val n2 = input("Confirme le nouveau", password = true)
        col.addView(old); col.addView(n1); col.addView(n2)
        col.addView(primaryButton("Enregistrer") {
            when {
                !checkPassword(old.text.toString()) -> toast("Mot de passe actuel incorrect.")
                n1.text.toString().length < 6 -> toast("Au moins 6 caractères.")
                n1.text.toString() != n2.text.toString() -> toast("Les deux ne correspondent pas.")
                else -> {
                    val salt = Security.newSalt()
                    prefs.salt = Security.encodeSalt(salt)
                    prefs.passwordHash = Security.hash(n1.text.toString().toCharArray(), salt)
                    toast("Mot de passe mis à jour.")
                    route()
                }
            }
        })
        col.addView(secondaryButton("Retour") { route() })
        setContentView(scroll)
    }

    /** Password prompt, then a forced countdown, then [onUnlocked]. */
    private fun startUnlockFlow(onUnlocked: () -> Unit) {
        val (scroll, col) = screen()
        col.addView(heading("Déverrouillage"))
        col.addView(body("Entre le mot de passe pour démarrer le compte à rebours."))
        val pw = input("Mot de passe", password = true)
        col.addView(pw)
        col.addView(primaryButton("Démarrer le compte à rebours") {
            if (!checkPassword(pw.text.toString())) {
                toast("Mot de passe incorrect.")
            } else {
                runCountdown(prefs.cooldownMinutes * 60, onUnlocked)
            }
        })
        col.addView(secondaryButton("Annuler") { route() })
        setContentView(scroll)
    }

    private fun runCountdown(seconds: Int, onDone: () -> Unit) {
        val (scroll, col) = screen()
        col.addView(heading("Patiente…"))
        val label = body("")
        col.addView(label)
        col.addView(secondaryButton("Annuler") {
            cancelCountdown()
            route()
        })
        setContentView(scroll)

        var remaining = seconds
        cancelCountdown()
        val r = object : Runnable {
            override fun run() {
                if (remaining <= 0) {
                    countdownRunnable = null
                    onDone()
                    return
                }
                val m = remaining / 60
                val s = remaining % 60
                label.text = "Déverrouillage dans %02d:%02d.\nReste sur cet écran : quitter annule.".format(m, s)
                remaining--
                handler.postDelayed(this, 1000)
            }
        }
        countdownRunnable = r
        handler.post(r)
    }

    private fun cancelCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
    }

    private fun renderUnlocked() {
        val (scroll, col) = screen()
        col.addView(heading("Protection déverrouillée"))
        col.addView(body("Tu as 5 minutes pour désactiver le service ou désinstaller l'app."))
        col.addView(primaryButton("Ouvrir les réglages d'accessibilité") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(secondaryButton("Préparer la désinstallation") {
            if (isAdminActive()) {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                try { dpm.removeActiveAdmin(AdminReceiver.component(this)) } catch (_: Exception) {}
            }
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            })
        })
        col.addView(secondaryButton("Retour") { route() })
        setContentView(scroll)
    }

    // --- View helpers --------------------------------------------------------

    private fun screen(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#FAF8F4")) }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 96)
        }
        scroll.addView(col)
        return scroll to col
    }

    private fun heading(t: String) = TextView(this).apply {
        text = t
        textSize = 28f
        setTextColor(Color.parseColor("#1F2933"))
        setPadding(0, 0, 0, 24)
    }

    private fun sectionTitle(t: String) = TextView(this).apply {
        text = t
        textSize = 18f
        setTextColor(Color.parseColor("#2B6E5F"))
        setPadding(0, 40, 0, 8)
    }

    private fun body(t: String) = TextView(this).apply {
        text = t
        textSize = 15f
        setTextColor(Color.parseColor("#3A3A3A"))
        setPadding(0, 0, 0, 12)
    }

    private fun input(hint: String, password: Boolean, number: Boolean = false) = EditText(this).apply {
        this.hint = hint
        inputType = when {
            password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            number -> InputType.TYPE_CLASS_NUMBER
            else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 16 }
    }

    private fun baseButton(t: String, bg: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(bg))
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = 8; bottomMargin = 8 }
    }

    private fun primaryButton(t: String, onClick: () -> Unit) = baseButton(t, "#2B6E5F", onClick)
    private fun secondaryButton(t: String, onClick: () -> Unit) = baseButton(t, "#5A6B73", onClick)
    private fun dangerButton(t: String, onClick: () -> Unit) = baseButton(t, "#C0502D", onClick)

    private fun statusRow(label: String, ok: Boolean, action: String, onClick: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }
        row.addView(TextView(this).apply {
            text = (if (ok) "✓ " else "✗ ") + label
            setTextColor(Color.parseColor(if (ok) "#2B6E5F" else "#C0502D"))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        if (!ok) {
            row.addView(Button(this).apply {
                text = action
                isAllCaps = false
                setOnClickListener { onClick() }
            })
        }
        return row
    }

    private fun patternRow(pattern: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = pattern
            textSize = 14f
            setTextColor(Color.parseColor("#1F2933"))
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        })
        row.addView(Button(this).apply {
            text = "Retirer"
            isAllCaps = false
            setOnClickListener {
                startUnlockFlow {
                    prefs.patterns = prefs.patterns.filter { it != pattern }
                    toast("Retiré.")
                    route()
                }
            }
        })
        return row
    }

    // --- Logic ---------------------------------------------------------------

    private fun checkPassword(input: String): Boolean {
        val salt = prefs.salt ?: return false
        val hash = prefs.passwordHash ?: return false
        val computed = Security.hash(input.toCharArray(), Security.decodeSalt(salt))
        return Security.slowEquals(computed, hash)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, BlockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun isAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(AdminReceiver.component(this))
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(this@MainActivity))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Empêche la désinstallation de FocusGuard sans passer par le déverrouillage."
            )
        }
        startActivity(intent)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        /** Reset when the process dies; the password is required again on next launch. */
        @JvmStatic
        var sessionUnlocked = false
    }
}
