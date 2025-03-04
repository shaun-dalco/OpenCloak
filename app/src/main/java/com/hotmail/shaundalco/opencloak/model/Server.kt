package com.hotmail.shaundalco.opencloak.model

class Server {
    var country: String? = null
    var flagUrl: String? = null
    var ovpn: String? = null
    var ovpnUserName: String? = null
    var ovpnUserPassword: String? = null


    constructor()

    constructor(country: String?, flagUrl: String?, ovpn: String?) {
        this.country = country
        this.flagUrl = flagUrl
        this.ovpn = ovpn
    }

    constructor(
        country: String?,
        flagUrl: String?,
        ovpn: String?,
        ovpnUserName: String?,
        ovpnUserPassword: String?
    ) {
        this.country = country
        this.flagUrl = flagUrl
        this.ovpn = ovpn
        this.ovpnUserName = ovpnUserName
        this.ovpnUserPassword = ovpnUserPassword
    }
}
