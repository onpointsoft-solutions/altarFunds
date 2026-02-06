# AltarFunds Member App - Enhanced UI Implementation

## 🎨 UI Enhancements Completed

### 1. Drawable Resources Created ✅
- `bg_gradient_primary.xml` - Purple gradient background (135° angle)
- `bg_rounded_white.xml` - White rounded background (24dp radius)
- `bg_button_rounded.xml` - Rounded button background (12dp radius)
- `bg_card_elevated.xml` - Card with border and rounded corners (16dp)

### 2. Enhanced Login Screen Design ✅
**Modern Features:**
- Gradient purple background covering full screen
- White logo with tint at top
- Large welcome text in white (32sp, bold)
- Subtitle in white with transparency
- **Card-based form** with 32dp rounded corners
- Elevated card (8dp elevation) containing all inputs
- Modern Material Design 3 components
- Smooth visual hierarchy

**Visual Structure:**
```
┌─────────────────────────┐
│   Gradient Background   │
│                         │
│      [White Logo]       │
│       Welcome!          │
│   Sign in to continue   │
│                         │
│  ┌───────────────────┐  │
│  │   White Card      │  │
│  │   [Email Input]   │  │
│  │   [Password]      │  │
│  │   [Login Button]  │  │
│  │   [Links]         │  │
│  └───────────────────┘  │
└─────────────────────────┘
```

### 3. Multi-Tabbed Registration (To Implement)

The registration will use **ViewPager2** with **TabLayout** for a modern multi-step experience:

**Tab 1: Personal Information**
- First Name
- Last Name
- Email
- Phone Number

**Tab 2: Church Information**
- Church Code (main requirement)
- Optional: Other Church Code
- Church search option

**Tab 3: Security**
- Password
- Confirm Password
- Terms acceptance

**Benefits:**
- Less overwhelming for users
- Better UX with step-by-step process
- Progress indicator
- Validation per tab
- Modern app feel

## 📱 Complete App Structure

### Core Components

#### 1. MainActivity with Bottom Navigation
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
    }
    
    private fun setupBottomNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        
        binding.bottomNav.setupWithNavController(navController)
    }
}
```

#### 2. Dashboard Fragment
```kotlin
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDashboardStats()
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    updateUI(stats)
                }
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }
}
```

#### 3. Giving Fragment with M-Pesa
```kotlin
class GivingFragment : Fragment() {
    private var _binding: FragmentGivingBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var donationAdapter: DonationAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFAB()
        loadDonations()
    }
    
    private fun setupFAB() {
        binding.fabGive.setOnClickListener {
            startActivity(Intent(requireContext(), GivingActivity::class.java))
        }
    }
    
    private fun loadDonations() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDonations()
                if (response.isSuccessful && response.body() != null) {
                    val donations = response.body()!!.results
                    donationAdapter.submitList(donations)
                }
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }
}
```

#### 4. Announcements Fragment
```kotlin
class AnnouncementsFragment : Fragment() {
    private var _binding: FragmentAnnouncementsBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var announcementAdapter: AnnouncementAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        loadAnnouncements()
    }
    
    private fun loadAnnouncements() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getAnnouncements()
                if (response.isSuccessful && response.body() != null) {
                    val announcements = response.body()!!.results
                    announcementAdapter.submitList(announcements)
                }
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }
}
```

#### 5. Devotionals Fragment
```kotlin
class DevotionalsFragment : Fragment() {
    private var _binding: FragmentDevotionalsBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var devotionalAdapter: DevotionalAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadDevotionals()
    }
    
    private fun loadDevotionals() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionals()
                if (response.isSuccessful && response.body() != null) {
                    val devotionals = response.body()!!.results
                    devotionalAdapter.submitList(devotionals)
                }
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }
}
```

#### 6. Profile Fragment
```kotlin
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadProfile()
        setupListeners()
    }
    
    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    displayProfile(user)
                }
            } catch (e: Exception) {
                showError(e.message)
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }
        
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }
}
```

### RecyclerView Adapters

#### DonationAdapter
```kotlin
class DonationAdapter : ListAdapter<Donation, DonationAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDonationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemDonationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(donation: Donation) {
            binding.tvAmount.text = donation.amount.formatCurrency()
            binding.tvType.text = donation.donationTypeDisplay
            binding.tvDate.text = donation.createdAt.formatDate()
            binding.tvStatus.text = donation.statusDisplay
            
            // Set status color
            val statusColor = when (donation.status) {
                "completed" -> R.color.success
                "pending" -> R.color.warning
                "failed" -> R.color.error
                else -> R.color.text_secondary
            }
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, statusColor)
            )
            
            binding.root.setOnClickListener {
                // Navigate to details
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Donation>() {
        override fun areItemsTheSame(oldItem: Donation, newItem: Donation) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Donation, newItem: Donation) =
            oldItem == newItem
    }
}
```

#### AnnouncementAdapter
```kotlin
class AnnouncementAdapter : ListAdapter<Announcement, AnnouncementAdapter.ViewHolder>(DiffCallback()) {
    
    inner class ViewHolder(private val binding: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(announcement: Announcement) {
            binding.tvTitle.text = announcement.title
            binding.tvContent.text = announcement.content
            binding.tvDate.text = announcement.createdAt.formatDate()
            binding.chipPriority.text = announcement.priorityDisplay
            
            // Set priority color
            val priorityColor = when (announcement.priority) {
                "urgent" -> R.color.priority_urgent
                "high" -> R.color.priority_high
                "medium" -> R.color.priority_medium
                "low" -> R.color.priority_low
                else -> R.color.text_secondary
            }
            binding.chipPriority.setChipBackgroundColorResource(priorityColor)
            
            binding.root.setOnClickListener {
                // Navigate to details
            }
        }
    }
}
```

#### DevotionalAdapter
```kotlin
class DevotionalAdapter : ListAdapter<Devotional, DevotionalAdapter.ViewHolder>(DiffCallback()) {
    
    inner class ViewHolder(private val binding: ItemDevotionalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(devotional: Devotional) {
            binding.tvTitle.text = devotional.title
            binding.tvScripture.text = devotional.scriptureReference
            binding.tvAuthor.text = "By ${devotional.author}"
            binding.tvDate.text = devotional.date.formatDate()
            binding.tvPreview.text = devotional.content.take(100) + "..."
            
            binding.root.setOnClickListener {
                // Navigate to details
            }
        }
    }
}
```

### Giving Activity with M-Pesa
```kotlin
class GivingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGivingBinding
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGivingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupListeners()
    }
    
    private fun setupListeners() {
        binding.btnGive.setOnClickListener {
            if (validateInput()) {
                initiateMpesaPayment()
            }
        }
    }
    
    private fun initiateMpesaPayment() {
        showLoading(true)
        
        val amount = binding.etAmount.text.toString()
        val phone = binding.etPhone.text.toString().formatPhoneNumber()
        val donationType = getSelectedDonationType()
        val description = binding.etDescription.text.toString()
        
        lifecycleScope.launch {
            try {
                val request = MpesaRequest(
                    phoneNumber = phone,
                    amount = amount,
                    donationType = donationType,
                    description = description
                )
                
                val response = app.apiService.initiateMpesaPayment(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val mpesaResponse = response.body()!!
                    showToast(mpesaResponse.customerMessage)
                    
                    // Check payment status after delay
                    checkPaymentStatus(mpesaResponse.checkoutRequestId)
                } else {
                    showToast("Failed to initiate payment")
                }
            } catch (e: Exception) {
                showToast(getString(R.string.network_error))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun checkPaymentStatus(checkoutRequestId: String) {
        // Poll for payment status
        lifecycleScope.launch {
            delay(10000) // Wait 10 seconds
            try {
                val response = app.apiService.checkPaymentStatus(checkoutRequestId)
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    showToast(status.message)
                    if (status.status == "completed") {
                        finish()
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
```

## 📋 Files to Create

### Layouts (18 files)
1. `activity_main.xml` - Bottom navigation host
2. `fragment_dashboard.xml` - Home screen
3. `fragment_giving.xml` - Donation history
4. `fragment_announcements.xml` - Announcements list
5. `fragment_devotionals.xml` - Devotionals list
6. `fragment_profile.xml` - User profile
7. `activity_giving.xml` - Make donation
8. `activity_donation_details.xml` - Donation details
9. `activity_announcement_details.xml` - Announcement details
10. `activity_devotional_details.xml` - Devotional details
11. `activity_church_search.xml` - Search churches
12. `activity_church_details.xml` - Church info
13. `activity_edit_profile.xml` - Edit profile
14. `activity_change_password.xml` - Change password
15. `activity_forgot_password.xml` - Password recovery
16. `item_donation.xml` - Donation list item
17. `item_announcement.xml` - Announcement list item
18. `item_devotional.xml` - Devotional list item

### Activities/Fragments (15 files)
1. `MainActivity.kt`
2. `DashboardFragment.kt`
3. `GivingFragment.kt`
4. `AnnouncementsFragment.kt`
5. `DevotionalsFragment.kt`
6. `ProfileFragment.kt`
7. `GivingActivity.kt`
8. `DonationDetailsActivity.kt`
9. `AnnouncementDetailsActivity.kt`
10. `DevotionalDetailsActivity.kt`
11. `ChurchSearchActivity.kt`
12. `ChurchDetailsActivity.kt`
13. `EditProfileActivity.kt`
14. `ChangePasswordActivity.kt`
15. `ForgotPasswordActivity.kt`

### Adapters (4 files)
1. `DonationAdapter.kt`
2. `AnnouncementAdapter.kt`
3. `DevotionalAdapter.kt`
4. `ChurchAdapter.kt`

### Navigation
1. `nav_graph.xml` - Navigation graph for fragments

## 🎯 Implementation Priority

1. **Phase 1: Core Navigation** (2 hours)
   - MainActivity with BottomNavigationView
   - Navigation graph
   - 5 main fragments (basic structure)

2. **Phase 2: Dashboard** (2 hours)
   - Dashboard fragment with stats
   - API integration
   - Card-based UI

3. **Phase 3: Giving** (3 hours)
   - Giving fragment with list
   - GivingActivity with M-Pesa
   - DonationAdapter
   - Payment status checking

4. **Phase 4: Content** (3 hours)
   - Announcements fragment and adapter
   - Devotionals fragment and adapter
   - Detail activities

5. **Phase 5: Profile** (2 hours)
   - Profile fragment
   - Edit profile activity
   - Change password activity

6. **Phase 6: Church** (2 hours)
   - Church search activity
   - Church details activity
   - Join church functionality

7. **Phase 7: Polish** (2 hours)
   - Forgot password
   - Error handling
   - Loading states
   - Testing

**Total Estimated Time: 16 hours**

## 🚀 Current Status

**Completed:**
- ✅ Project infrastructure (100%)
- ✅ API layer (100%)
- ✅ Data models (100%)
- ✅ Utilities (100%)
- ✅ Resources (100%)
- ✅ Enhanced login screen (100%)
- ✅ Registration with church code (100%)
- ✅ Drawable resources (100%)

**In Progress:**
- 🚧 Multi-tabbed registration
- 🚧 Main app screens

**Remaining:**
- ⏳ All fragments and activities
- ⏳ All adapters
- ⏳ All layouts

The foundation is solid. All backend integration is ready. The remaining work is UI implementation following the patterns established in the authentication screens.
