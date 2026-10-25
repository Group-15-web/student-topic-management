package vn.edu.detaihub.controller.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import vn.edu.detaihub.dto.Forms;
import vn.edu.detaihub.dto.Views;
import vn.edu.detaihub.exception.RuleException;
import vn.edu.detaihub.service.QueryService;

@ExtendWith(MockitoExtension.class)
class FormViewSupportTest {
  private static final String TYPE = "groups";
  private static final String TITLE = "Tạo nhóm thực hiện";

  @Mock private QueryService query;
  @Mock private Runnable work;
  @Mock private Function<List<Views.Option>, List<FormViewSupport.Field>> fields;

  private FormViewSupport support;
  private Forms.Group form;
  private BeanPropertyBindingResult binding;
  private ExtendedModelMap model;
  private RedirectAttributesModelMap redirect;

  @BeforeEach
  void setUp() {
    support = new FormViewSupport(query);
    form = new Forms.Group();
    form.setName("Nhóm đã nhập");
    form.setCycleId(7L);
    binding = new BeanPropertyBindingResult(form, "form");
    model = new ExtendedModelMap();
    redirect = new RedirectAttributesModelMap();
  }

  @Test
  void fieldFactoriesKeepTheirTypesOptionsAndMultipleSelectHint() {
    var options = List.of(new Views.Option(3L, "Giảng viên"));
    var text = FormViewSupport.field("name", "Tên nhóm", "text");
    var single = FormViewSupport.select("cycleId", "Đợt đăng ký", options, false);
    var multiple = FormViewSupport.select("memberIds", "Thành viên", options, true);

    assertThat(text)
        .isEqualTo(new FormViewSupport.Field("name", "Tên nhóm", "text", List.of(), ""));
    assertThat(single.name()).isEqualTo("cycleId");
    assertThat(single.label()).isEqualTo("Đợt đăng ký");
    assertThat(single.type()).isEqualTo("select");
    assertThat(single.options()).isSameAs(options);
    assertThat(single.hint()).isEmpty();
    assertThat(multiple.type()).isEqualTo("multiple");
    assertThat(multiple.options()).isSameAs(options);
    assertThat(multiple.hint()).isEqualTo("Giữ Ctrl để chọn nhiều người.");
    verifyNoInteractions(query, work, fields);
  }

  @Test
  void showKeepsTheBoundFormAndMapsOrderedCyclesToFieldOptions() {
    when(query.cycles()).thenReturn(List.of(cycle(7L, "Đợt mới"), cycle(2L, "Đợt trước")));
    var options = List.of(new Views.Option(7L, "Đợt mới"), new Views.Option(2L, "Đợt trước"));
    var renderedFields =
        List.of(FormViewSupport.select("cycleId", "Đợt đăng ký", options, false));
    when(fields.apply(options)).thenReturn(renderedFields);

    String view = support.show(TYPE, TITLE, form, model, fields);

    assertThat(view).isEqualTo("form");
    assertThat(model.get("form")).isSameAs(form);
    assertThat(model.get("fields")).isSameAs(renderedFields);
    assertThat(model).containsEntry("type", TYPE).containsEntry("title", TITLE);
    verify(query).cycles();
    verify(fields).apply(options);
    verifyNoInteractions(work);
  }

  @Test
  void successfulSaveRunsWorkAndRedirectsWithoutRenderingFieldsOrLoadingCycles() {
    String view = support.save(TYPE, TITLE, form, binding, model, redirect, work, fields);

    assertThat(view).isEqualTo("redirect:/groups");
    assertThat(redirect.getFlashAttributes().get("success"))
        .isEqualTo("Đã lưu dữ liệu thành công.");
    assertThat(model).isEmpty();
    verify(work).run();
    verifyNoInteractions(query, fields);
  }

  @Test
  void bindingErrorsSkipWorkAndPreserveBothTheSubmittedFormAndBindingResult() {
    binding.rejectValue("name", "invalid", "Tên nhóm không hợp lệ.");
    model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "form", binding);
    when(query.cycles()).thenReturn(List.of());
    var renderedFields = List.of(FormViewSupport.field("name", "Tên nhóm", "text"));
    when(fields.apply(anyList())).thenReturn(renderedFields);

    String view = support.save(TYPE, TITLE, form, binding, model, redirect, work, fields);

    assertThat(view).isEqualTo("form");
    assertThat(model.get("form")).isSameAs(form);
    assertThat(model.get(BindingResult.MODEL_KEY_PREFIX + "form")).isSameAs(binding);
    assertThat(binding.getFieldError("name").getRejectedValue()).isEqualTo("Nhóm đã nhập");
    assertThat(model.get("fields")).isSameAs(renderedFields);
    assertThat(model).containsEntry("type", TYPE).containsEntry("title", TITLE);
    assertThat(redirect.getFlashAttributes()).isEmpty();
    verifyNoInteractions(work);
  }

  @Test
  void ruleExceptionRendersTheSameFormWithItsDomainMessage() {
    doThrow(new RuleException("Sinh viên đã có nhóm.")).when(work).run();
    when(query.cycles()).thenReturn(List.of());
    when(fields.apply(anyList())).thenReturn(List.of());

    String view = support.save(TYPE, TITLE, form, binding, model, redirect, work, fields);

    assertThat(view).isEqualTo("form");
    assertThat(model.get("form")).isSameAs(form);
    assertThat(model)
        .containsEntry("error", "Sinh viên đã có nhóm.")
        .containsEntry("type", TYPE)
        .containsEntry("title", TITLE);
    assertThat(redirect.getFlashAttributes()).isEmpty();
    verify(work).run();
    verify(query).cycles();
    verify(fields).apply(List.of());
  }

  @Test
  void integrityFailureRendersAFriendlyMessageWithoutExposingTheDatabaseError() {
    doThrow(new DataIntegrityViolationException("SQL constraint internal_secret_index"))
        .when(work)
        .run();
    when(query.cycles()).thenReturn(List.of());
    when(fields.apply(anyList())).thenReturn(List.of());

    String view = support.save(TYPE, TITLE, form, binding, model, redirect, work, fields);

    assertThat(view).isEqualTo("form");
    assertThat(model.get("form")).isSameAs(form);
    assertThat(model.get("error"))
        .isEqualTo(
            "Dữ liệu bị trùng hoặc đang được tham chiếu. Kiểm tra mã, email và các liên kết.");
    assertThat(model.toString()).doesNotContain("internal_secret_index");
    assertThat(redirect.getFlashAttributes()).isEmpty();
    verify(work).run();
    verify(query).cycles();
  }

  @Test
  void accessDeniedIsPropagatedToTheExistingExceptionAdvice() {
    var failure = new AccessDeniedException("Không có quyền thực hiện.");
    doThrow(failure).when(work).run();

    assertThatThrownBy(
            () -> support.save(TYPE, TITLE, form, binding, model, redirect, work, fields))
        .isSameAs(failure);

    assertThat(model).isEmpty();
    assertThat(redirect.getFlashAttributes()).isEmpty();
    verify(work).run();
    verifyNoInteractions(query, fields);
  }

  @Test
  void doneUsesTheExistingActionFlashRatherThanTheFormSaveFlash() {
    String view = FormViewSupport.done("/groups/12", redirect);

    assertThat(view).isEqualTo("redirect:/groups/12");
    assertThat(redirect.getFlashAttributes().get("success"))
        .isEqualTo("Đã lưu thay đổi thành công.");
    assertThat(redirect.getFlashAttributes().get("success"))
        .isNotEqualTo("Đã lưu dữ liệu thành công.");
    verifyNoInteractions(query, work, fields);
  }

  private Views.Cycle cycle(Long id, String name) {
    return new Views.Cycle(
        id, name, "COURSE", "ACTIVE", "LECTURER",
        "", "", "", "", "", "", "", 3, 5, 0, false);
  }
}
