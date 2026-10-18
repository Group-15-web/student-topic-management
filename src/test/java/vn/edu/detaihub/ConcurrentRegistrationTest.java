package vn.edu.detaihub;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.model.*;
import vn.edu.detaihub.repository.DomainRepository;
import vn.edu.detaihub.service.GroupService;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1}",
      "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}",
      "spring.datasource.username=${TEST_DB_USER:sa}",
      "spring.datasource.password=${TEST_DB_PASSWORD:}",
      "app.mail-worker=false"
    })
class ConcurrentRegistrationTest {
  @Autowired TransactionTemplate tx;
  @Autowired DomainRepository db;
  @Autowired UserDetailsService users;
  @Autowired GroupService groups;

  @Test
  void twoGroupsCannotTakeLastPlace() throws Exception {
    var ids =
        tx.execute(
            s -> {
              var result = new ArrayList<Long>();
              for (int start : List.of(7, 10)) {
                var leader =
                    db.all(Student.class).stream()
                        .filter(x -> x.getCode().equals("SV%03d".formatted(start)))
                        .findFirst()
                        .orElseThrow();
                var g = new StudentGroup();
                g.setName("Concurrency " + start);
                g.setCycleId(2L);
                g.setLeaderId(leader.getId());
                g.setStatus("FORMING");
                db.save(g);
                for (int i = start; i < start + 3; i++) {
                  String code = "SV%03d".formatted(i);
                  var student =
                      db.all(Student.class).stream()
                          .filter(x -> x.getCode().equals(code))
                          .findFirst()
                          .orElseThrow();
                  student.setActiveGroupId(g.getId());
                  var m = new GroupMember();
                  m.setGroupId(g.getId());
                  m.setStudentId(student.getId());
                  m.setStatus("ACTIVE");
                  db.save(m);
                }
                result.add(g.getId());
              }
              return result;
            });
    var barrier = new CyclicBarrier(2);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < 2; i++) {
        final int index = i;
        futures.add(
            executor.submit(
                () -> {
                  var user =
                      users.loadUserByUsername((index == 0 ? "sv7" : "sv10") + "@demo.local");
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          new UsernamePasswordAuthenticationToken(
                              user, null, user.getAuthorities()));
                  try {
                    barrier.await(10, TimeUnit.SECONDS);
                    var f = new Forms.Registration();
                    f.setGroupId(ids.get(index));
                    f.setTopicId(1L);
                    groups.register(f);
                    return true;
                  } catch (vn.edu.detaihub.exception.RuleException e) {
                    assertThat(e.getMessage()).contains("hết chỗ");
                    return false;
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      }
      int successes = 0;
      for (var future : futures) if (future.get(20, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(1);
    }
    long count =
        tx.execute(
            s ->
                db.all(TopicRegistration.class).stream()
                    .filter(r -> r.getTopicId().equals(1L) && r.getStatus().equals("PENDING"))
                    .count());
    assertThat(count).isEqualTo(1);
  }
}
