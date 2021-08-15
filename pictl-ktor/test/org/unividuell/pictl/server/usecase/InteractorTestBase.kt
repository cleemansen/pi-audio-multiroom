package org.unividuell.pictl.server.usecase

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

abstract class InteractorTestBase {

    init {
        stopKoin()
    }

    fun startInjection(module: Module) = startKoin {
        modules(
            module
        )
    }
}