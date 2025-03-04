package com.hotmail.shaundalco.opencloak

import android.content.Context
import android.content.SharedPreferences
import com.hotmail.shaundalco.opencloak.model.Server
import com.hotmail.shaundalco.opencloak.Utils.getImgURL

class SharedPreference(private val context: Context) {
    private val mPreference: SharedPreferences
    private val mPrefEditor: SharedPreferences.Editor

    init {
        this.mPreference = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        this.mPrefEditor = mPreference.edit()
    }

    /**
     * Save server details
     * @param server details of ovpn server
     */
    fun saveServer(server: Server) {
        mPrefEditor.putString(SERVER_COUNTRY, server.country)
        mPrefEditor.putString(SERVER_FLAG, server.flagUrl)
        mPrefEditor.putString(SERVER_OVPN, server.ovpn)
        mPrefEditor.putString(SERVER_OVPN_USER, server.ovpnUserName)
        mPrefEditor.putString(SERVER_OVPN_PASSWORD, server.ovpnUserPassword)
        mPrefEditor.commit()
    }

    val server: Server
        /**
         * Get server data from shared preference
         * @return server model object
         */
        get() {
            val server = Server(
                mPreference.getString(SERVER_COUNTRY, "Japan"),
                mPreference.getString(SERVER_FLAG, getImgURL(R.drawable.test)),
                mPreference.getString(SERVER_OVPN, "japan.ovpn"),
                mPreference.getString(SERVER_OVPN_USER, "vpn"),
                mPreference.getString(SERVER_OVPN_PASSWORD, "vpn")
            )

            return server
        }

    companion object {
        private const val APP_PREFS_NAME = "CakeVPNPreference"

        private const val SERVER_COUNTRY = "server_country"
        private const val SERVER_FLAG = "server_flag"
        private const val SERVER_OVPN = "server_ovpn"
        private const val SERVER_OVPN_USER = "server_ovpn_user"
        private const val SERVER_OVPN_PASSWORD = "server_ovpn_password"
    }
}
