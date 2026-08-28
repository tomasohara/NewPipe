package net.newpipe.app.platform

import org.koin.core.annotation.Singleton

@Singleton(binds = [BuildInfo::class])
class JVMBuildInfo : BuildInfo {
    // "Apk" is an Android-specific concept; desktop has no signed release
    // artifact to check, so this is always false
    override val isReleaseApk: Boolean = false

    // Bug fix: this was hardcoded false, which silently hid the Debug
    // settings category (SettingsCategoryType.DEBUG is filtered on
    // isDebug) on desktop even in dev builds. No separate release build
    // variant exists for the desktop target yet, so every desktop build
    // is currently a debug build.
    override val isDebug: Boolean = true
}
