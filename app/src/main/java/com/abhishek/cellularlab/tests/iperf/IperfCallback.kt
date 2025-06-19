package com.abhishek.cellularlab.tests.iperf

interface IperfCallback {
    fun onOutput(line: String)
    fun onError(error: String)
    fun onComplete()
}