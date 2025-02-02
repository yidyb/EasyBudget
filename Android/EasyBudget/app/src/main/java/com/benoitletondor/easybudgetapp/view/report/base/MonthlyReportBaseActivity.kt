/*
 *   Copyright 2023 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.view.report.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.benoitletondor.easybudgetapp.databinding.ActivityMonthlyReportBinding
import com.benoitletondor.easybudgetapp.helper.BaseActivity
import com.benoitletondor.easybudgetapp.helper.getMonthTitle
import com.benoitletondor.easybudgetapp.helper.launchCollect
import com.benoitletondor.easybudgetapp.helper.removeButtonBorder
import com.benoitletondor.easybudgetapp.view.report.MonthlyReportFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

/**
 * Activity that displays monthly report
 *
 * @author Benoit LETONDOR
 */
@AndroidEntryPoint
class MonthlyReportBaseActivity : BaseActivity<ActivityMonthlyReportBinding>(), ViewPager.OnPageChangeListener {
    private val viewModel: MonthlyReportBaseViewModel by viewModels()

    private var ignoreNextPageSelectedEvent: Boolean = false

    override fun createBinding(): ActivityMonthlyReportBinding = ActivityMonthlyReportBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.monthlyReportPreviousMonthButton.text = "<"
        binding.monthlyReportNextMonthButton.text = ">"

        binding.monthlyReportPreviousMonthButton.setOnClickListener {
            viewModel.onPreviousMonthButtonClicked()
        }

        binding.monthlyReportNextMonthButton.setOnClickListener {
            viewModel.onNextMonthButtonClicked()
        }

        binding.monthlyReportPreviousMonthButton.removeButtonBorder()
        binding.monthlyReportNextMonthButton.removeButtonBorder()

        var loadedDates: List<LocalDate> = emptyList()
        lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            when(state) {
                is MonthlyReportBaseViewModel.State.Loaded -> {
                    binding.monthlyReportProgressBar.visibility = View.GONE
                    binding.monthlyReportContent.visibility = View.VISIBLE

                    if (state.dates != loadedDates) {
                        loadedDates = state.dates
                        configureViewPager(state.dates)
                    }

                    if( !ignoreNextPageSelectedEvent ) {
                        binding.monthlyReportViewPager.setCurrentItem(state.selectedPosition.position, true)
                    }

                    ignoreNextPageSelectedEvent = false

                    binding.monthlyReportMonthTitleTv.text = state.selectedPosition.date.getMonthTitle(this)

                    // Last and first available month
                    val isFirstMonth = state.selectedPosition.position == 0

                    binding.monthlyReportNextMonthButton.isEnabled = !state.selectedPosition.latest
                    binding.monthlyReportPreviousMonthButton.isEnabled = !isFirstMonth
                }
                MonthlyReportBaseViewModel.State.Loading -> {
                    binding.monthlyReportProgressBar.visibility = View.VISIBLE
                    binding.monthlyReportContent.visibility = View.GONE
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Configure the [.pager] adapter and listener.
     */
    private fun configureViewPager(dates: List<LocalDate>) {
        binding.monthlyReportViewPager.removeOnPageChangeListener(this)

        binding.monthlyReportViewPager.offscreenPageLimit = 0
        binding.monthlyReportViewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return MonthlyReportFragment.newInstance(dates[position])
            }

            override fun getCount(): Int {
                return dates.size
            }
        }
        binding.monthlyReportViewPager.addOnPageChangeListener(this)
    }

// ------------------------------------------>

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        ignoreNextPageSelectedEvent = true

        viewModel.onPageSelected(position)
    }

    override fun onPageScrollStateChanged(state: Int) {}

    companion object {
        /**
         * Extra to add the the launch intent to specify that user comes from the notification (used to
         * show not the current month but the last one)
         */
        const val FROM_NOTIFICATION_EXTRA = "fromNotif"
    }
}
