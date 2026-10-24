package vn.edu.detaihub.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.detaihub.dto.*;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.security.ActorService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryService {
  public List<Views.Progress> councilProgress(Long registrationId) {
    var assignment = assessment.councilAssignment(registrationId);
    if (assignment.isEmpty()) return List.of();
    var members =
        db.all(CouncilMember.class).stream()
            .filter(m -> m.getCouncilId().equals(assignment.get().getCouncilId()))
            .toList();
    boolean allowed =
        actor.faculty()
            || members.stream()
                .anyMatch(
                    m ->
                        Set.of("PRESIDENT", "SECRETARY").contains(m.getMemberRole())
                            && db.get(Lecturer.class, m.getLecturerId())
                                .getUserId()
                                .equals(actor.user().getId()));
    if (!allowed) return List.of();
    return members.stream()
        .map(
            m ->
                new Views.Progress(
                    lecturerName(m.getLecturerId()),
                    m.getMemberRole(),
                    db.all(Evaluation.class).stream()
                        .filter(
                            e ->
                                e.getRegistrationId().equals(registrationId)
                                    && e.getLecturerId().equals(m.getLecturerId()))
                        .map(Evaluation::getStatus)
                        .findFirst()
                        .orElse("MISSING")))
        .toList();
  }

  private final DomainRepository db;
  private final ActorService actor;
  private final TopicService topics;
  private final GroupService groups;
  private final AssessmentService assessment;
  private final AnnouncementService announcements;
  private final StudentGroupRepository groupRepo;
  private final GroupMemberRepository memberRepo;
  private final StudentRepository studentRepo;
  private final TopicRegistrationRepository registrationRepo;
  private final TopicRepository topicRepo;
  private final RegistrationCycleRepository cycleRepo;
  private final SubmissionRepository submissionRepo;
  private final EvaluationResultRepository evaluationResultRepo;
  private final UserAccountRepository userAccountRepo;

  private String utcTime(LocalDateTime d) {
    return d == null
        ? "—"
        : time(
            d.atOffset(java.time.ZoneOffset.UTC)
                .atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDateTime());
  }

  private String time(LocalDateTime d) {
    return d == null ? "—" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
  }

  private String userName(Long id) {
    return id == null ? "—" : db.get(UserAccount.class, id).getFullName();
  }

  private String lecturerName(Long id) {
    return userName(db.get(Lecturer.class, id).getUserId());
  }

  public Views.User me() {
    var u = actor.user();
    return new Views.User(u.getId(), u.getFullName(), u.getEmail(), actor.roles(u.getId()));
  }

  public List<Views.Option> departments() {
    return db.all(Department.class).stream()
        .map(d -> new Views.Option(d.getId(), d.getName()))
        .toList();
  }

  public List<Views.Option> lecturers() {
    actor.need("FACULTY_MANAGER", "SYSTEM_ADMIN", "LECTURER");
    return db.all(Lecturer.class).stream()
        .filter(l -> db.get(UserAccount.class, l.getUserId()).getEnabled())
        .map(l -> new Views.Option(l.getId(), l.getCode() + " · " + userName(l.getUserId())))
        .toList();
  }

  public List<Views.Option> lecturerUsers() {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    return db.all(Lecturer.class).stream()
        .map(l -> new Views.Option(l.getUserId(), userName(l.getUserId())))
        .toList();
  }

  public List<Views.Cycle> cycles() {
    return db.all(RegistrationCycle.class).stream()
        .filter(c -> actor.faculty() || !c.getStatus().equals("DRAFT"))
        .map(this::cycleView)
        .toList();
  }

  private Views.Cycle cycleView(RegistrationCycle c) {
    String phase =
        !TopicService.running(c)
            ? c.getStatus()
            : TopicService.between(c.getLecturerStart(), c.getLecturerEnd())
                ? "GV đề xuất"
                : TopicService.between(c.getStudentStart(), c.getStudentEnd())
                    ? "SV đăng ký"
                    : TopicService.now().isBefore(c.getLecturerStart())
                        ? "Sắp mở"
                        : "Thực hiện / đánh giá";
    return new Views.Cycle(
        c.getId(),
        c.getName(),
        c.getCycleType(),
        c.getStatus(),
        phase,
        time(c.getLecturerStart()),
        time(c.getLecturerEnd()),
        time(c.getStudentStart()),
        time(c.getStudentEnd()),
        time(c.getSubmissionDeadline()),
        time(c.getReviewerDeadline()),
        time(c.getCouncilDate()),
        c.getMinMembers(),
        c.getMaxMembers(),
        c.getReviewerCount(),
        c.getCouncilRequired());
  }

  public Forms.Cycle cycleForm(Long id) {
    actor.need("FACULTY_MANAGER");
    var f = new Forms.Cycle();
    if (id != null) BeanUtils.copyProperties(db.get(RegistrationCycle.class, id), f);
    return f;
  }

  public List<Views.Topic> topics() {
    return db.all(Topic.class).stream().filter(topics::visible).map(this::topicView).toList();
  }

  public Views.Topic topic(Long id) {
    var t = db.get(Topic.class, id);
    actor.allow(topics.visible(t));
    return topicView(t);
  }

  private Views.Topic topicView(Topic t) {
    boolean edit =
        (actor.faculty() || t.getAuthorId().equals(actor.user().getId()))
            && Set.of("DRAFT", "RETURNED", "REJECTED").contains(t.getStatus());
    String names =
        String.join(
            ", ",
            db.all(TopicSupervisor.class).stream()
                .filter(s -> s.getTopicId().equals(t.getId()))
                .map(s -> lecturerName(s.getLecturerId()))
                .toList());
    long occupied =
        db.all(TopicRegistration.class).stream()
            .filter(
                r ->
                    r.getTopicId().equals(t.getId())
                        && Set.of("APPROVED", "PENDING").contains(r.getStatus()))
            .count();
    return new Views.Topic(
        t.getId(),
        t.getTitle(),
        t.getDescription(),
        t.getStatus(),
        t.getCycleId(),
        db.get(RegistrationCycle.class, t.getCycleId()).getName(),
        t.getDepartmentId(),
        db.get(Department.class, t.getDepartmentId()).getName(),
        userName(t.getAuthorId()),
        names.isBlank() ? "Chưa phân công" : names,
        t.getMaxGroups(),
        occupied,
        edit,
        edit,
        actor.faculty(),
        actor.departmentHead(t.getDepartmentId()),
        t.getDecisionNote());
  }

  public Forms.Topic topicForm(Long id) {
    actor.need("LECTURER", "FACULTY_MANAGER");
    var f = new Forms.Topic();
    if (id != null) {
      var t = topic(id);
      actor.allow(t.canEdit());
      BeanUtils.copyProperties(db.get(Topic.class, id), f);
      f.setSupervisorIds(
          db.all(TopicSupervisor.class).stream()
              .filter(s -> s.getTopicId().equals(id))
              .map(TopicSupervisor::getLecturerId)
              .toList());
    }
    return f;
  }

  public List<Views.Group> groups() {
    var visible = groups.visibleGroups();
    if (visible.isEmpty()) return List.of();

    var groupIds = visible.stream().map(StudentGroup::getId).toList();
    var cycleIds = visible.stream().map(StudentGroup::getCycleId).distinct().toList();
    var cycles = cycleRepo.findAllById(cycleIds).stream()
        .collect(java.util.stream.Collectors.toMap(RegistrationCycle::getId, c -> c));

    var memberItems = memberRepo.findMemberItemsByGroupIdInAndStatusIn(groupIds, List.of("ACTIVE", "INVITED"));
    var membersByGroup = memberItems.stream()
        .collect(java.util.stream.Collectors.groupingBy(Views.MemberItem::groupId));

    Long currentUserId = actor.user().getId();

    return visible.stream().map(g -> {
      var c = cycles.get(g.getCycleId());
      String cycleName = c == null ? "—" : c.getName();
      int minMembers = c == null ? 0 : c.getMinMembers();
      int maxMembers = c == null ? 0 : c.getMaxMembers();

      var items = membersByGroup.getOrDefault(g.getId(), List.of());
      var members = items.stream().map(item -> new Views.Member(
          item.id(),
          item.code(),
          item.name(),
          item.status(),
          g.getLeaderId().equals(item.id()),
          currentUserId.equals(item.userId())
      )).toList();

      boolean lead = members.stream().anyMatch(m -> m.leader() && m.me());
      boolean own = members.stream().anyMatch(m -> m.me() && "ACTIVE".equals(m.status()));
      boolean invited = members.stream().anyMatch(m -> m.me() && "INVITED".equals(m.status()));

      return new Views.Group(
          g.getId(),
          g.getName(),
          g.getStatus(),
          g.getCycleId(),
          cycleName,
          minMembers,
          maxMembers,
          lead,
          own,
          invited,
          g.getStatus().equals("FORMING")
              && c != null
              && TopicService.running(c)
              && TopicService.now().isBefore(c.getStudentEnd()),
          members);
    }).toList();
  }

  public Views.Group group(Long id) {
    var g = groupRepo.findById(id)
        .orElseThrow(() -> new vn.edu.detaihub.exception.RuleException("Không tìm thấy nhóm.", 404));
    actor.allow(groups.canViewGroup(g));
    var c = cycleRepo.findById(g.getCycleId()).orElse(null);
    String cycleName = c == null ? "—" : c.getName();
    int minMembers = c == null ? 0 : c.getMinMembers();
    int maxMembers = c == null ? 0 : c.getMaxMembers();

    var memberItems = memberRepo.findMemberItemsByGroupIdInAndStatusIn(List.of(g.getId()), List.of("ACTIVE", "INVITED"));
    Long currentUserId = actor.user().getId();

    var members = memberItems.stream().map(item -> new Views.Member(
        item.id(),
        item.code(),
        item.name(),
        item.status(),
        g.getLeaderId().equals(item.id()),
        currentUserId.equals(item.userId())
    )).toList();

    boolean lead = members.stream().anyMatch(m -> m.leader() && m.me());
    boolean own = members.stream().anyMatch(m -> m.me() && "ACTIVE".equals(m.status()));
    boolean invited = members.stream().anyMatch(m -> m.me() && "INVITED".equals(m.status()));

    return new Views.Group(
        g.getId(),
        g.getName(),
        g.getStatus(),
        g.getCycleId(),
        cycleName,
        minMembers,
        maxMembers,
        lead,
        own,
        invited,
        g.getStatus().equals("FORMING")
            && c != null
            && TopicService.running(c)
            && TopicService.now().isBefore(c.getStudentEnd()),
        members);
  }

  public List<Views.Registration> registrations() {
    var visible = groups.visibleRegistrations();
    if (visible.isEmpty()) return List.of();

    var regIds = visible.stream().map(TopicRegistration::getId).toList();
    var items = registrationRepo.findRegistrationItemsByIdIn(regIds).stream()
        .collect(java.util.stream.Collectors.toMap(Views.RegistrationItem::id, item -> item));

    var currentStudent = studentRepo.findByUserId(actor.user().getId());
    Long currentStudentId = currentStudent.map(Student::getId).orElse(null);

    return visible.stream().map(r -> {
      var item = items.get(r.getId());
      if (item == null) return null;

      var ca = assessment.councilAssignment(r.getId());
      var council = ca.map(a -> db.get(ReviewCouncil.class, a.getCouncilId()));
      String score = null;
      if (maySeeResult(r)) {
        score = evaluationResultRepo.findByRegistrationIdAndStatus(r.getId(), "PUBLISHED")
            .map(e -> e.getFinalScore().toString())
            .orElse(null);
      }
      boolean isLeader = currentStudentId != null && currentStudentId.equals(item.leaderId());

      return new Views.Registration(
          item.id(),
          item.groupId(),
          item.groupName(),
          item.topicId(),
          item.topicTitle(),
          item.cycleId(),
          item.cycleName() != null ? item.cycleName() : "—",
          item.status(),
          item.note(),
          isLeader,
          actor.faculty(),
          council.map(ReviewCouncil::getName).orElse("Chưa phân công"),
          council.map(c -> time(c.getReportAt()) + " · " + c.getLocation()).orElse("—"),
          council.map(ReviewCouncil::getId).orElse(null),
          ca.map(a -> assessment.president(a.getCouncilId())).orElse(false),
          ca.map(a -> a.getConfirmedAt() != null).orElse(false),
          score);
    }).filter(Objects::nonNull).toList();
  }

  public Views.Registration registration(Long id) {
    var r = registrationRepo.findById(id)
        .orElseThrow(() -> new vn.edu.detaihub.exception.RuleException("Không tìm thấy đơn đăng ký.", 404));
    actor.allow(groups.canView(r));

    var item = registrationRepo.findRegistrationItemById(id)
        .orElseThrow(() -> new vn.edu.detaihub.exception.RuleException("Không tìm thấy đơn đăng ký.", 404));

    var ca = assessment.councilAssignment(id);
    var council = ca.map(a -> db.get(ReviewCouncil.class, a.getCouncilId()));
    String score = null;
    if (maySeeResult(r)) {
      score = evaluationResultRepo.findByRegistrationIdAndStatus(r.getId(), "PUBLISHED")
          .map(e -> e.getFinalScore().toString())
          .orElse(null);
    }

    var currentStudent = studentRepo.findByUserId(actor.user().getId());
    Long currentStudentId = currentStudent.map(Student::getId).orElse(null);
    boolean isLeader = currentStudentId != null && currentStudentId.equals(item.leaderId());

    return new Views.Registration(
        item.id(),
        item.groupId(),
        item.groupName(),
        item.topicId(),
        item.topicTitle(),
        item.cycleId(),
        item.cycleName() != null ? item.cycleName() : "—",
        item.status(),
        item.note(),
        isLeader,
        actor.faculty(),
        council.map(ReviewCouncil::getName).orElse("Chưa phân công"),
        council.map(c -> time(c.getReportAt()) + " · " + c.getLocation()).orElse("—"),
        council.map(ReviewCouncil::getId).orElse(null),
        ca.map(a -> assessment.president(a.getCouncilId())).orElse(false),
        ca.map(a -> a.getConfirmedAt() != null).orElse(false),
        score);
  }

  public List<Views.Submission> submissions(Long registrationId) {
    registration(registrationId);
    return submissionRepo.findByRegistrationId(registrationId).stream()
        .map(
            s ->
                new Views.Submission(
                    s.getId(),
                    s.getVersionNo(),
                    s.getFileName(),
                    s.getFileSize(),
                    utcTime(s.getCreatedAt())))
        .toList();
  }

  private boolean maySeeGrade(Evaluation e) {
    if (actor.faculty()) return true;
    var l = db.get(Lecturer.class, e.getLecturerId());
    if (l.getUserId().equals(actor.user().getId())) return true;
    if (e.getEvaluationType().equals("COUNCIL")
        && assessment
            .councilAssignment(e.getRegistrationId())
            .map(a -> assessment.president(a.getCouncilId()))
            .orElse(false)) return true;
    return false;
  }

  public List<Views.Evaluation> evaluations(Long registrationId) {
    return db.all(Evaluation.class).stream()
        .filter(e -> registrationId == null || e.getRegistrationId().equals(registrationId))
        .filter(this::maySeeGrade)
        .map(
            e -> {
              var r = db.get(TopicRegistration.class, e.getRegistrationId());
              return new Views.Evaluation(
                  e.getId(),
                  r.getId(),
                  db.get(StudentGroup.class, r.getGroupId()).getName(),
                  db.get(Topic.class, r.getTopicId()).getTitle(),
                  lecturerName(e.getLecturerId()),
                  e.getEvaluationType(),
                  e.getStatus(),
                  e.getScore() == null ? "—" : e.getScore().toString(),
                  e.getComment(),
                  db.get(Lecturer.class, e.getLecturerId())
                      .getUserId()
                      .equals(actor.user().getId()));
            })
        .toList();
  }

  public Views.Evaluation evaluation(Long id) {
    return evaluations(null).stream()
        .filter(e -> e.id().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new vn.edu.detaihub.exception.RuleException(
                    "Không tìm thấy phiếu thuộc quyền truy cập.", 404));
  }

  public List<Views.Council> councils() {
    return db.all(ReviewCouncil.class).stream()
        .filter(assessment::councilVisible)
        .map(
            c ->
                new Views.Council(
                    c.getId(),
                    c.getName(),
                    db.get(RegistrationCycle.class, c.getCycleId()).getName(),
                    time(c.getReportAt()),
                    c.getLocation(),
                    db.all(CouncilMember.class).stream()
                        .filter(m -> m.getCouncilId().equals(c.getId()))
                        .map(m -> lecturerName(m.getLecturerId()) + " · " + m.getMemberRole())
                        .toList(),
                    assessment.president(c.getId())))
        .toList();
  }

  private boolean maySeeResult(TopicRegistration r) {
    return actor.faculty()
        || groups.member(r.getGroupId())
        || topics.supervises(r.getTopicId(), actor.user().getId())
        || db.all(Evaluation.class).stream()
            .anyMatch(
                e ->
                    e.getRegistrationId().equals(r.getId())
                        && db.get(Lecturer.class, e.getLecturerId())
                            .getUserId()
                            .equals(actor.user().getId()));
  }

  public List<Views.Result> results() {
    return db.all(EvaluationResult.class).stream()
        .filter(
            e -> {
              var r = db.get(TopicRegistration.class, e.getRegistrationId());
              var t = db.get(Topic.class, r.getTopicId());
              return actor.faculty()
                  || e.getStatus().equals("PUBLISHED")
                      && groups.canView(r)
                      && (!actor.departmentHead(t.getDepartmentId())
                          || topics.supervises(t.getId(), actor.user().getId())
                          || groups.member(r.getGroupId())
                          || db.all(Evaluation.class).stream()
                              .anyMatch(
                                  x ->
                                      x.getRegistrationId().equals(r.getId())
                                          && db.get(Lecturer.class, x.getLecturerId())
                                              .getUserId()
                                              .equals(actor.user().getId())));
            })
        .map(
            e -> {
              var r = db.get(TopicRegistration.class, e.getRegistrationId());
              var t = db.get(Topic.class, r.getTopicId());
              return new Views.Result(
                  e.getId(),
                  r.getId(),
                  t.getTitle(),
                  db.get(StudentGroup.class, r.getGroupId()).getName(),
                  t.getCycleId(),
                  db.get(RegistrationCycle.class, t.getCycleId()).getName(),
                  e.getFinalScore().toString(),
                  e.getStatus(),
                  time(e.getPublishedAt()));
            })
        .toList();
  }

  public List<Views.Announcement> announcements() {
    return db.all(Announcement.class).stream()
        .filter(announcements::visible)
        .map(
            a ->
                new Views.Announcement(
                    a.getId(),
                    a.getTitle(),
                    a.getContent(),
                    userName(a.getAuthorId()),
                    a.getAudience(),
                    a.getStatus(),
                    utcTime(a.getCreatedAt()),
                    actor.admin()
                        || actor.faculty()
                        || a.getAuthorId().equals(actor.user().getId())
                            && a.getStatus().equals("DRAFT")))
        .toList();
  }

  public Forms.Announcement announcementForm(Long id) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER", "DEPARTMENT_HEAD");
    var f = new Forms.Announcement();
    if (id != null) {
      actor.allow(announcements().stream().anyMatch(a -> a.id().equals(id) && a.editable()));
      BeanUtils.copyProperties(db.get(Announcement.class, id), f);
    }
    return f;
  }

  public List<Views.Account> accounts() {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER", "DEPARTMENT_HEAD");
    return db.all(UserAccount.class).stream()
        .filter(
            u ->
                actor.admin()
                    || actor.faculty()
                        && (actor.roles(u.getId()).contains("STUDENT")
                            || actor.roles(u.getId()).contains("LECTURER"))
                    || db.all(Lecturer.class).stream()
                        .anyMatch(
                            l ->
                                l.getUserId().equals(u.getId())
                                    && actor.departmentHead(l.getDepartmentId())))
        .map(
            u -> {
              var s =
                  db.all(Student.class).stream()
                      .filter(x -> x.getUserId().equals(u.getId()))
                      .findFirst();
              var l =
                  db.all(Lecturer.class).stream()
                      .filter(x -> x.getUserId().equals(u.getId()))
                      .findFirst();
              return new Views.Account(
                  u.getId(),
                  u.getFullName(),
                  u.getEmail(),
                  s.map(Student::getCode).orElse(l.map(Lecturer::getCode).orElse("—")),
                  l.map(x -> db.get(Department.class, x.getDepartmentId()).getName()).orElse("—"),
                  String.join(", ", actor.roles(u.getId())),
                  u.getEnabled());
            })
        .toList();
  }

  public Forms.Account accountForm(Long id) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    var f = new Forms.Account();
    if (id != null) {
      actor.allow(accounts().stream().anyMatch(a -> a.id().equals(id)));
      var u = db.get(UserAccount.class, id);
      f.setId(id);
      f.setEmail(u.getEmail());
      f.setFullName(u.getFullName());
      f.setEnabled(u.getEnabled());
      var roles = actor.roles(id);
      f.setRole(
          List.of("SYSTEM_ADMIN", "FACULTY_MANAGER", "DEPARTMENT_HEAD", "LECTURER", "STUDENT")
              .stream()
              .filter(roles::contains)
              .findFirst()
              .orElse("STUDENT"));
      db.all(Student.class).stream()
          .filter(s -> s.getUserId().equals(id))
          .findFirst()
          .ifPresent(
              s -> {
                f.setCode(s.getCode());
                f.setIntake(s.getIntake());
              });
      db.all(Lecturer.class).stream()
          .filter(l -> l.getUserId().equals(id))
          .findFirst()
          .ifPresent(
              l -> {
                f.setCode(l.getCode());
                f.setDepartmentId(l.getDepartmentId());
              });
      if (f.getCode() == null) f.setCode("ADMIN");
    }
    return f;
  }

  public List<Views.Department> departmentRows() {
    return db.all(Department.class).stream()
        .map(
            d ->
                new Views.Department(
                    d.getId(), d.getCode(), d.getName(), userName(d.getHeadUserId())))
        .toList();
  }

  public Forms.Department departmentForm(Long id) {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    var f = new Forms.Department();
    if (id != null) BeanUtils.copyProperties(db.get(Department.class, id), f);
    return f;
  }

  public List<Views.Mail> mails() {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    return db.all(EmailDelivery.class).stream()
        .map(
            m ->
                new Views.Mail(
                    m.getId(),
                    m.getRecipient(),
                    actor.admin() && !actor.faculty() ? "Thông báo kết quả" : m.getSubject(),
                    m.getStatus(),
                    m.getAttempts(),
                    m.getLastError(),
                    actor.faculty() && m.getStatus().equals("PREVIEW")))
        .toList();
  }

  public List<Views.Audit> audits() {
    actor.need("SYSTEM_ADMIN", "FACULTY_MANAGER");
    return db.all(AuditLog.class).stream()
        .filter(
            a ->
                actor.faculty()
                    || !Set.of("GRADE_SNAPSHOT", "REOPEN_RESULT").contains(a.getAction()))
        .limit(300)
        .map(
            a ->
                new Views.Audit(
                    a.getId(),
                    userName(a.getActorId()),
                    a.getAction(),
                    a.getObjectType(),
                    a.getObjectId(),
                    actor.faculty() ? a.getDetails() : "",
                    utcTime(a.getCreatedAt())))
        .toList();
  }

  public static <T> Views.Page<T> page(List<T> rows, String query, int page) {
    String q = query == null ? "" : query.trim();
    var filtered =
        rows.stream()
            .filter(
                r ->
                    q.isEmpty()
                        || r.toString()
                            .toLowerCase(Locale.ROOT)
                            .contains(q.toLowerCase(Locale.ROOT)))
            .toList();
    int total = Math.max(1, (filtered.size() + 11) / 12);
    int n = Math.max(1, Math.min(page, total));
    return new Views.Page<>(
        filtered.subList(
            Math.min((n - 1) * 12, filtered.size()), Math.min(n * 12, filtered.size())),
        n,
        total,
        filtered.size(),
        q);
  }
}
