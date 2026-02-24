# Sanctum Church Management System - Desktop Application Specification

## Overview
A comprehensive Java desktop application for church management using Swing/JavaFX for the GUI and JDBC for database connectivity. This application provides offline capabilities with synchronization to the web backend.

## Technology Stack
- **Language**: Java 17+
- **GUI Framework**: JavaFX 17+ (preferred) or Swing
- **Database**: SQLite (local) + MySQL/PostgreSQL (sync)
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **Architecture**: MVC Pattern
- **Reporting**: JasperReports
- **Charts**: JFreeChart

## Project Structure
```
sanctum-desktop/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sanctum/
│   │   │           ├── Main.java
│   │   │           ├── controller/
│   │   │           ├── model/
│   │   │           ├── view/
│   │   │           ├── service/
│   │   │           ├── repository/
│   │   │           ├── util/
│   │   │           └── config/
│   │   └── resources/
│   │       ├── fxml/
│   │       ├── css/
│   │       ├── images/
│   │       └── reports/
│   └── test/
└── pom.xml
```

## Core Class Models

### 1. User Management Models

#### User.java
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "church_id")
    private Church church;
    
    // Getters, setters, constructors
}

public enum UserRole {
    SUPER_ADMIN, DENOMINATION_ADMIN, CHURCH_ADMIN, PASTOR, TREASURER, SECRETARY, MEMBER
}
```

#### Church.java
```java
@Entity
@Table(name = "churches")
public class Church {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private ChurchType churchType;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Column(nullable = false)
    private String email;
    
    private String website;
    
    @Column(nullable = false)
    private String addressLine1;
    
    private String addressLine2;
    
    @Column(nullable = false)
    private String city;
    
    @Column(nullable = false)
    private String county;
    
    private String postalCode;
    
    private String seniorPastorName;
    private String seniorPastorPhone;
    private String seniorPastorEmail;
    
    private LocalDate establishedDate;
    
    private Integer membershipCount;
    private Integer averageAttendance;
    
    private String registrationNumber;
    private LocalDate registrationDate;
    
    private String primaryColor = "#3B82F6";
    private String secondaryColor = "#10B981";
    private String accentColor = "#F59E0B";
    
    private String logoPath;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Getters, setters, constructors
}

public enum ChurchType {
    MAIN, BRANCH, PLANT, CHAPLAINCY, MISSION_STATION
}
```

### 2. Member Management Models

#### Member.java
```java
@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    private String middleName;
    
    @Column(unique = true)
    private String memberNumber;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    private String email;
    
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Enumerated(EnumType.STRING)
    private MaritalStatus maritalStatus;
    
    private String occupation;
    
    @Column(name = "join_date")
    private LocalDate joinDate;
    
    @Enumerated(EnumType.STRING)
    private MembershipStatus status;
    
    @Column(name = "baptism_date")
    private LocalDate baptismDate;
    
    private String baptismLocation;
    
    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;
    
    private String notes;
    
    @ManyToOne
    @JoinColumn(name = "church_id")
    private Church church;
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<FamilyMember> familyMembers = new ArrayList<>();
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Donation> donations = new ArrayList<>();
    
    // Getters, setters, constructors
}

public enum Gender {
    MALE, FEMALE, OTHER
}

public enum MaritalStatus {
    SINGLE, MARRIED, DIVORCED, WIDOWED, SEPARATED
}

public enum MembershipStatus {
    ACTIVE, INACTIVE, TRANSFERRED, DECEASED
}
```

#### FamilyMember.java
```java
@Entity
@Table(name = "family_members")
public class FamilyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    private String relationship;
    
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    private String phoneNumber;
    
    private String occupation;
    
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
    
    // Getters, setters, constructors
}
```

### 3. Financial Management Models

#### Donation.java
```java
@Entity
@Table(name = "donations")
public class Donation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private DonationType donationType;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    private String checkNumber;
    
    private LocalDate donationDate;
    
    private String receiptNumber;
    
    private String description;
    
    private Boolean isTaxDeductible = true;
    
    private Boolean isAnonymous = false;
    
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
    
    @ManyToOne
    @JoinColumn(name = "church_id")
    private Church church;
    
    @ManyToOne
    @JoinColumn(name = "fund_id")
    private Fund fund;
    
    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
    
    // Getters, setters, constructors
}

public enum DonationType {
    TITHE, OFFERING, SPECIAL, BUILDING, MISSION, SEED, OTHER
}

public enum PaymentMethod {
    CASH, CHECK, BANK_TRANSFER, MOBILE_MONEY, CREDIT_CARD, OTHER
}
```

#### Expense.java
```java
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String description;
    
    private String vendor;
    
    private String invoiceNumber;
    
    private LocalDate expenseDate;
    
    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;
    
    private String receiptPath;
    
    private Boolean isApproved = false;
    
    @ManyToOne
    @JoinColumn(name = "church_id")
    private Church church;
    
    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
    
    // Getters, setters, constructors
}

public enum ExpenseCategory {
    SALARIES, UTILITIES, RENT, MAINTENANCE, SUPPLIES, MARKETING, INSURANCE, TAXES, OTHER
}
```

#### Fund.java
```java
@Entity
@Table(name = "funds")
public class Fund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    private String purpose;
    
    private BigDecimal targetAmount;
    
    private BigDecimal currentAmount = BigDecimal.ZERO;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    private FundStatus status;
    
    @ManyToOne
    @JoinColumn(name = "church_id")
    private Church church;
    
    // Getters, setters, constructors
}

public enum FundStatus {
    ACTIVE, INACTIVE, COMPLETED, CANCELLED
}
```

### 4. Event Management Models

#### Event.java
```java
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime startDateTime;
    
    private LocalDateTime endDateTime;
    
    private String location;
    
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    private BigDecimal expectedAttendance;
    
    private BigDecimal actualAttendance;
    
    private String notes;
    
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    
    @ManyToOne
    @JoinColumn(name = "church_id")
    private Church church;
    
    @ManyToOne
    @JoinColumn(name = "organized_by")
    private User organizedBy;
    
    @ManyToMany
    @JoinTable(
        name = "event_attendees",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private Set<Member> attendees = new HashSet<>();
    
    // Getters, setters, constructors
}

public enum EventType {
    SERVICE, MEETING, CONFERENCE, RETREAT, FUNDRAISER, OUTREACH, TRAINING, SOCIAL
}

public enum EventStatus {
    PLANNED, ONGOING, COMPLETED, CANCELLED
}
```

## Core Functionality Classes

### 1. Service Layer

#### AuthService.java
```java
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationException("User not found"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }
        
        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is inactive");
        }
        
        return user;
    }
    
    public User register(UserRegistrationDTO dto) {
        // Validation and user creation logic
    }
    
    public void logout() {
        // Clear session
    }
    
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        // Password change logic
    }
}
```

#### MemberService.java
```java
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    
    public Member createMember(MemberDTO dto) {
        // Member creation logic with validation
    }
    
    public Member updateMember(Long id, MemberDTO dto) {
        // Member update logic
    }
    
    public void deleteMember(Long id) {
        // Soft delete logic
    }
    
    public List<Member> searchMembers(String query, Long churchId) {
        // Search functionality
    }
    
    public void importMembersFromExcel(MultipartFile file) {
        // Excel import logic
    }
    
    public void exportMembersToExcel(Long churchId) {
        // Excel export logic
    }
}
```

#### DonationService.java
```java
@Service
public class DonationService {
    private final DonationRepository donationRepository;
    private final MemberRepository memberRepository;
    private final FundRepository fundRepository;
    
    public Donation recordDonation(DonationDTO dto) {
        // Donation recording with receipt generation
    }
    
    public List<Donation> getDonationsByDateRange(LocalDate start, LocalDate end, Long churchId) {
        // Date range filtering
    }
    
    public DonationSummary getDonationSummary(Long churchId, LocalDate start, LocalDate end) {
        // Summary statistics
    }
    
    public void generateDonationReport(Long churchId, LocalDate start, LocalDate end) {
        // Report generation
    }
}
```

### 2. Repository Layer (JPA)

#### UserRepository.java
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByChurchIdAndIsActive(Long churchId, Boolean isActive);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
```

#### MemberRepository.java
```java
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByChurchIdAndStatus(Long churchId, MembershipStatus status);
    List<Member> findByChurchIdAndFirstNameContainingIgnoreCase(Long churchId, String name);
    List<Member> findByChurchIdAndJoinDateBetween(Long churchId, LocalDate start, LocalDate end);
    Optional<Member> findByMemberNumber(String memberNumber);
}
```

### 3. GUI Controllers (JavaFX)

#### MainController.java
```java
@FXML
public class MainController {
    @FXML private BorderPane mainPane;
    @FXML private Label userLabel;
    @FXML private Label churchLabel;
    @FXML private Button dashboardButton;
    @FXML private Button membersButton;
    @FXML private Button donationsButton;
    @FXML private Button eventsButton;
    @FXML private Button reportsButton;
    @FXML private Button settingsButton;
    
    private User currentUser;
    private Church currentChurch;
    
    @FXML
    public void initialize() {
        setupEventHandlers();
        loadUserSession();
    }
    
    @FXML
    public void handleDashboard() {
        loadView("/fxml/dashboard.fxml");
    }
    
    @FXML
    public void handleMembers() {
        loadView("/fxml/members.fxml");
    }
    
    @FXML
    public void handleDonations() {
        loadView("/fxml/donations.fxml");
    }
    
    @FXML
    public void handleEvents() {
        loadView("/fxml/events.fxml");
    }
    
    @FXML
    public void handleReports() {
        loadView("/fxml/reports.fxml");
    }
    
    @FXML
    public void handleSettings() {
        loadView("/fxml/settings.fxml");
    }
    
    @FXML
    public void handleLogout() {
        // Logout logic
    }
    
    private void loadView(String fxmlPath) {
        // View loading logic
    }
}
```

#### MemberController.java
```java
@FXML
public class MemberController {
    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, String> firstNameColumn;
    @FXML private TableColumn<Member, String> lastNameColumn;
    @FXML private TableColumn<Member, String> phoneColumn;
    @FXML private TableColumn<Member, String> emailColumn;
    @FXML private TableColumn<Member, MembershipStatus> statusColumn;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<MembershipStatus> statusFilter;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    
    private final MemberService memberService;
    private final ObservableList<Member> memberList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadMembers();
        setupFilters();
    }
    
    @FXML
    public void handleAddMember() {
        // Open add member dialog
    }
    
    @FXML
    public void handleEditMember() {
        // Open edit member dialog
    }
    
    @FXML
    public void handleDeleteMember() {
        // Delete member with confirmation
    }
    
    @FXML
    public void handleSearch() {
        // Search functionality
    }
    
    @FXML
    public void handleExport() {
        // Export to Excel
    }
    
    @FXML
    public void handleImport() {
        // Import from Excel
    }
}
```

### 4. Utility Classes

#### DatabaseManager.java
```java
@Component
public class DatabaseManager {
    private final EntityManagerFactory entityManagerFactory;
    
    public void initializeDatabase() {
        // Database initialization logic
    }
    
    public void backupDatabase(String backupPath) {
        // Database backup
    }
    
    public void restoreDatabase(String backupPath) {
        // Database restore
    }
    
    public void syncWithServer() {
        // Synchronization with web backend
    }
    
    public boolean isOnline() {
        // Check internet connectivity
    }
}
```

#### ReportGenerator.java
```java
@Component
public class ReportGenerator {
    public void generateDonationReport(Long churchId, LocalDate start, LocalDate end, String outputPath) {
        // JasperReports donation report
    }
    
    public void generateMemberReport(Long churchId, MembershipStatus status, String outputPath) {
        // JasperReports member report
    }
    
    public void generateFinancialSummary(Long churchId, LocalDate start, LocalDate end, String outputPath) {
        // Financial summary report
    }
    
    public void generateAttendanceReport(Long eventId, String outputPath) {
        // Event attendance report
    }
}
```

## Key Features Implementation

### 1. Authentication & Security
- JWT token-based authentication
- Role-based access control
- Password encryption with BCrypt
- Session management
- Audit logging

### 2. Data Synchronization
- Offline-first approach with SQLite
- Bidirectional sync with web backend
- Conflict resolution strategies
- Progress indicators for sync operations

### 3. Reporting & Analytics
- Real-time dashboards
- Custom report builder
- Chart generation (donations, attendance, growth)
- PDF/Excel export capabilities
- Scheduled reports

### 4. Data Import/Export
- Excel import for bulk data
- CSV export for analysis
- Backup and restore functionality
- Data validation during import

### 5. Notifications & Alerts
- System notifications
- Email notifications (if configured)
- Birthday reminders
- Follow-up reminders

## Configuration Files

### application.properties
```properties
# Database Configuration
database.url=jdbc:sqlite:sanctum.db
database.driver=org.sqlite.JDBC
database.username=
database.password=

# Server Configuration
server.url=https://sanctum.co.ke/backend
api.timeout=30000

# Application Configuration
app.name=Sanctum Desktop
app.version=1.0.0
app.theme=default

# Security Configuration
security.jwt.secret=your-secret-key
security.jwt.expiration=86400000

# Reporting Configuration
reports.template.path=/reports/
reports.output.path=/exports/
```

## Deployment

### Build Configuration (pom.xml)
```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.36.0.3</version>
    </dependency>
    
    <!-- JPA/Hibernate -->
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-core</artifactId>
        <version>5.6.14.Final</version>
    </dependency>
    
    <!-- Reporting -->
    <dependency>
        <groupId>net.sf.jasperreports</groupId>
        <artifactId>jasperreports</artifactId>
        <version>6.20.0</version>
    </dependency>
</dependencies>
```

## Installation & Setup

1. **Prerequisites**
   - Java 17 or higher
   - Minimum 4GB RAM
   - 500MB disk space

2. **Installation Steps**
   - Download installer executable
   - Run installer with administrator privileges
   - Follow setup wizard
   - Configure database connection
   - Create admin account

3. **Configuration**
   - Set server URL for synchronization
   - Configure backup settings
   - Set user preferences
   - Import initial data if needed

## Testing Strategy

### Unit Tests
- Service layer business logic
- Repository data access
- Utility functions
- Validation rules

### Integration Tests
- Database operations
- API synchronization
- Report generation
- File operations

### UI Tests
- User workflows
- Form validation
- Navigation
- Data display

## Maintenance & Support

### Regular Updates
- Security patches
- Feature enhancements
- Bug fixes
- Performance improvements

### Backup Strategy
- Automated daily backups
- Manual backup options
- Cloud backup integration
- Data recovery procedures

### User Support
- Built-in help system
- Video tutorials
- User manual
- Technical support contact

This specification provides a comprehensive foundation for developing a robust Java desktop application for church management with all necessary features and functionality.
