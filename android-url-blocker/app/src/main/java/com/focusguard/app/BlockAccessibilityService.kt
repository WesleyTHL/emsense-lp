package com.focusguard.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Watches browsers (by reading the address bar) and the YouTube app (by detecting
 * the Shorts player). On a match it covers the screen with a full-screen overlay
 * whose only action is to go home. While locked, it also guards the screens that
 * would let the user disable the service or uninstall the app.
 */
class BlockAccessibilityService : AccessibilityService() {

    private lateinit var prefs: Prefs
    private val main = Handler(Looper.getMainLooper())

    private var cachedPatterns: List<String> = emptyList()
    private var rules = BlockRules(emptyList())

    private var overlay: View? = null
    private var lastEventAt = 0L
    private var lastToastAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val now = System.currentTimeMillis()
        if (now - lastEventAt < 200) return
        lastEventAt = now

        if (!::prefs.isInitialized) prefs = Prefs(this)

        if (pkg in GUARD_PACKAGES) {
            guardSettings()
            return
        }

        val blocked = when {
            isBrowser(pkg) -> {
                val r = rulesFor()
                urlCandidates(pkg).any { r.matches(it) }
            }
            pkg in YOUTUBE_PACKAGES -> prefs.blockYouTubeShorts && isShorts()
            else -> false
        }

        if (blocked) showBlockOverlay() else removeOverlay()
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        removeOverlay()
        return super.onUnbind(intent)
    }

    // --- Rules ---------------------------------------------------------------

    private fun rulesFor(): BlockRules {
        val p = prefs.patterns
        if (p != cachedPatterns) {
            cachedPatterns = p
            rules = BlockRules(p)
        }
        return rules
    }

    // --- URL detection -------------------------------------------------------

    private fun urlCandidates(pkg: String): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val out = ArrayList<String>(4)
        BROWSER_URL_BAR_IDS[pkg]?.let { id ->
            root.findAccessibilityNodeInfosByViewId(id)?.forEach { node ->
                node.text?.toString()?.takeIf { it.isNotBlank() }?.let(out::add)
            }
        }
        if (out.isEmpty()) collectUrlLikeText(root, out, 0)
        return out
    }

    private fun collectUrlLikeText(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        node ?: return
        if (depth > 25 || out.size > 12) return
        val t = node.text?.toString()
        if (t != null && t.length in 4..2048 && !t.contains(' ') && t.contains('.')) {
            out.add(t)
        }
        for (i in 0 until node.childCount) {
            collectUrlLikeText(node.getChild(i), out, depth + 1)
        }
    }

    // --- YouTube Shorts detection -------------------------------------------

    private fun isShorts(): Boolean {
        val root = rootInActiveWindow ?: return false
        for (id in SHORTS_VIEW_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) return true
        }
        return false
    }

    // --- Self-protection -----------------------------------------------------

    /** While locked, back out of any system screen that mentions this app. */
    private fun guardSettings() {
        if (prefs.isUnlocked) return
        val root = rootInActiveWindow ?: return
        if (treeMentionsApp(root, 0)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            toast("Déverrouille FocusGuard (mot de passe) avant de le modifier.")
        }
    }

    private fun treeMentionsApp(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        node ?: return false
        if (depth > 30) return false
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()
        if (text?.contains("focusguard") == true || desc?.contains("focusguard") == true) return true
        for (i in 0 until node.childCount) {
            if (treeMentionsApp(node.getChild(i), depth + 1)) return true
        }
        return false
    }

    // --- Block overlay -------------------------------------------------------

    private fun showBlockOverlay() {
        if (overlay != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#C0502D"))
            setPadding(64, 64, 64, 64)
            isClickable = true
        }
        root.addView(TextView(this).apply {
            text = "🛡️"
            textSize = 56f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Bloqué"
            setTextColor(Color.WHITE)
            textSize = 30f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "Cette page est bloquée par FocusGuard."
            setTextColor(Color.parseColor("#FFE6DD"))
            textSize = 16f
            gravity = Gravity.CENTER
        })
        root.addView(Button(this).apply {
            text = "Revenir à l'accueil"
            setOnClickListener { goHome() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 48 })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        )
        params.gravity = Gravity.CENTER

        try {
            wm.addView(root, params)
            overlay = root
        } catch (e: Exception) {
            // Fall back to a hard navigation away if the overlay can't be added.
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun removeOverlay() {
        val v = overlay ?: return
        overlay = null
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (e: Exception) {
        }
    }

    private fun goHome() {
        removeOverlay()
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun toast(msg: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastAt < 2000) return
        lastToastAt = now
        main.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        private val YOUTUBE_PACKAGES = setOf("com.google.android.youtube")

        private val GUARD_PACKAGES = setOf(
            "com.android.settings",
            "com.samsung.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.securitycenter"
        )

        private val BROWSER_URL_BAR_IDS = mapOf(
            "com.android.chrome" to "com.android.chrome:id/url_bar",
            "com.chrome.beta" to "com.chrome.beta:id/url_bar",
            "com.chrome.dev" to "com.chrome.dev:id/url_bar",
            "com.brave.browser" to "com.brave.browser:id/url_bar",
            "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
            "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.focus" to "org.mozilla.focus:id/mozac_browser_toolbar_url_view",
            "com.opera.browser" to "com.opera.browser:id/url_field",
            "com.opera.mini.native" to "com.opera.mini.native:id/url_field",
            "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.duckduckgo.mobile.android" to "com.duckduckgo.mobile.android:id/omnibarTextInput",
            "com.kiwibrowser.browser" to "com.kiwibrowser.browser:id/url_bar"
        )

        private val SHORTS_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_watch_player",
            "com.google.android.youtube:id/reel_player_underlay",
            "com.google.android.youtube:id/reel_progress_bar",
            "com.google.android.youtube:id/shorts_video_cell"
        )

        fun isBrowser(pkg: String): Boolean =
            BROWSER_URL_BAR_IDS.containsKey(pkg) ||
                pkg.contains("browser") ||
                pkg.contains("chrome")
    }
}
