package jp.plesion.app

import android.app.Application
import jp.plesion.app.di.AppContainer

/**
 * アプリ全体で1つだけ存在するオブジェクト。依存関係のコンテナを保持する。
 * Firebase の初期化が必要になったらここで行う（docs/FIREBASE_SETUP.md 参照）。
 */
class PlesionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
