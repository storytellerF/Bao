package com.storyteller_f.bao_library

@Suppress(
    "unused",
    "MemberVisibilityCanBePrivate",
    "ConstPropertyName",
    "SpellCheckingInspection"
)
object LinuxSig {
    const val hup = 1
    const val int = 2
    const val quit = 3
    const val ill = 4
    const val trap = 5
    const val abrt = 6
    const val bus = 7
    const val fpe = 8
    const val kill = 9
    const val usr1 = 10
    const val segv = 11
    const val usr2 = 12
    const val pipe = 13
    const val alrm = 14
    const val term = 15
    const val stkflt = 16
    const val chld = 17
    const val cont = 18
    const val stop = 19
    const val tstp = 20
    const val ttin = 21
    const val ttou = 22
    const val urg = 23
    const val xcpu = 24
    const val xfsz = 25
    const val vtalrm = 26
    const val prof = 27
    const val winch = 28
    const val io = 29
    const val pwr = 30
    const val sys = 31
    val appFatalSig = listOf(
        abrt,
        bus,
        fpe,
        ill,
        int,
        pipe,
        segv,
        term
    )
    val ignoreSig = listOf(
        chld,
        urg,
        winch
    )
    val terminateSig =
        listOf(
            hup,
            int,
            kill,
            usr1,
            usr1,
            usr2,
            pipe,
            alrm,
            term,
            stkflt,
            vtalrm,
            prof,
            io,
            pwr
        )
    val coreSig = listOf(
        quit,
        ill,
        trap,
        abrt,
        bus,
        fpe,
        segv,
        xcpu,
        xfsz,
        sys
    )
    val stopSig = listOf(
        stop,
        tstp,
        ttin,
        ttou
    )
    val continueSig = listOf(cont)
}