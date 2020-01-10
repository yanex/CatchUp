/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.ui.activity

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import com.uber.autodispose.autoDispose
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.InjectingBaseActivity
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.databinding.ActivityMainBinding
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.service.api.ScrollableContent
import io.sweers.catchup.ui.DetailDisplayer
import io.sweers.catchup.ui.fragments.PagerFragment
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.dimming.TintPainter
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.InterceptResult
import me.saket.inboxrecyclerview.page.SimplePageStateChangeCallbacks
import javax.inject.Inject
import javax.inject.Qualifier

class MainActivity : InjectingBaseActivity() {

  @Inject
  internal lateinit var customTab: CustomTabActivityHelper
  @Inject
  internal lateinit var linkManager: LinkManager
  @Inject
  internal lateinit var syllabus: Syllabus
  @Inject
  internal lateinit var fragmentFactory: FragmentFactory

  internal lateinit var detailPage: ExpandablePageLayout
  internal var pagerFragment: PagerFragment? = null

  override fun setFragmentFactory() {
    supportFragmentManager.fragmentFactory = fragmentFactory
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    syllabus.bind(this)
    lifecycle()
        .doOnStart(linkManager) { connect(this@MainActivity) }
        .doOnStart(customTab) { bindCustomTabsService(this@MainActivity) }
        .doOnStop(customTab) { unbindCustomTabsService(this@MainActivity) }
        .doOnDestroy(customTab) { connectionCallback = null }
        .autoDispose(this)
        .subscribe()

    val binding = viewContainer.inflateBinding(ActivityMainBinding::inflate)
    detailPage = binding.detailPage

    if (savedInstanceState == null) {
      supportFragmentManager.commitNow {
        add(R.id.fragment_container, PagerFragment().also { pagerFragment = it })
      }
    } else {
      pagerFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as PagerFragment
    }
  }
}

@Qualifier
annotation class TextViewPool

@Qualifier
annotation class VisualViewPool

@Qualifier
annotation class FinalServices

/**
 * A displayer that repeatedly shows new detail views in a new ExpandablePageLayout.
 */
@PerActivity
class MainActivityDetailDisplayer @Inject constructor(
  private val mainActivity: MainActivity
) : DetailDisplayer {

  private var collapser: (() -> Unit)? = null

  private inline val detailPage get() = mainActivity.detailPage

  override val isExpandedOrExpanding get() = detailPage.isExpandedOrExpanding

  override fun showDetail(body: (ExpandablePageLayout, FragmentManager) -> () -> Unit) {
    collapser?.invoke()
    collapser = null
    detailPage.addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
      override fun onPageCollapsed() {
        detailPage.removeStateChangeCallbacks(this)
        collapser = null
      }
    })
    collapser = body(detailPage, mainActivity.supportFragmentManager)
  }

  override fun bind(irv: InboxRecyclerView, useExistingFragment: Boolean) {
    val targetFragment = if (useExistingFragment) {
      mainActivity.supportFragmentManager.findFragmentById(detailPage.id)
    } else {
      null
    }
    bind(irv, targetFragment)
  }

  override fun bind(irv: InboxRecyclerView, targetFragment: Fragment?) {
    // TODO this is ugly, do better
    mainActivity.pagerFragment?.appBarLayout?.let {
      detailPage.pushParentToolbarOnExpand(it)
    }
    irv.expandablePage = detailPage
    irv.tintPainter = TintPainter.uncoveredArea(
        color = ContextCompat.getColor(irv.context, R.color.colorPrimary),
        opacity = 0.65F
    )
    if (targetFragment is ScrollableContent) {
      detailPage.pullToCollapseInterceptor = { _, _, upwardPull ->
        val directionInt = if (upwardPull) +1 else -1
        val canScrollFurther = targetFragment.canScrollVertically(directionInt)
        if (canScrollFurther) InterceptResult.INTERCEPTED else InterceptResult.IGNORED
      }
    }
  }

  override fun unbind(irv: InboxRecyclerView) {
    detailPage.pullToCollapseInterceptor = null
    irv.expandablePage = null
  }
}
