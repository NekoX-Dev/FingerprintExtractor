package io.nekohasekai.tg.fingerprintextractor

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import cn.hutool.core.util.ReflectUtil
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedMain : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        if (lpparam.packageName != "org.telegram.messenger") return

        val alertsCreator =
            Class.forName("org.telegram.ui.Components.AlertsCreator", false, lpparam.classLoader)
        val createSimpleAlert = ReflectUtil.getMethod(
            alertsCreator,
            "createSimpleAlert",
            Context::class.java,
            String::class.java,
            String::class.java
        )

        val androidUtilities =
            Class.forName("org.telegram.messenger.AndroidUtilities", false, lpparam.classLoader)
        val getCertificateSHA256Fingerprint =
            ReflectUtil.getMethod(androidUtilities, "getCertificateSHA256Fingerprint")
        val addToClipboard =
            ReflectUtil.getMethod(androidUtilities, "addToClipboard", String::class.java)

        val localeController =
            Class.forName("org.telegram.messenger.LocaleController", false, lpparam.classLoader)
        val getString =
            ReflectUtil.getMethod(localeController, "getString", String::class.java)

        XposedHelpers.findAndHookMethod(
            "org.telegram.ui.LaunchActivity",
            lpparam.classLoader,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val ctx = param.thisObject as Activity
                    val fingerprint =
                        ReflectUtil.invokeStatic<String>(getCertificateSHA256Fingerprint)

                    val alert = ReflectUtil.invokeStatic<Any>(
                        createSimpleAlert,
                        ctx,
                        "Fingerprint",
                        fingerprint
                    )

                    ReflectUtil.invoke<Any>(alert, "setNegativeButton",
                        ReflectUtil.invokeStatic<String>(getString, "Copy"),
                        DialogInterface.OnClickListener { dialog, which ->
                            ReflectUtil.invokeStatic<Any>(addToClipboard, fingerprint)
                            Toast.makeText(
                                ctx,
                                ReflectUtil.invokeStatic<String>(getString, "TextCopied"),
                                Toast.LENGTH_SHORT
                            ).show()
                        })

                    ReflectUtil.invoke<Any>(alert, "show")
                }
            })

    }
}