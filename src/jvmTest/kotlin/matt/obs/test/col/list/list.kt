package matt.obs.test.col.list

import matt.obs.col.olist.basicROObservableListOf
import matt.test.Tests
import kotlin.test.Test


class ObsListTests : Tests() {

    @Test fun initObjects() {
        basicROObservableListOf(1)
    }
}
