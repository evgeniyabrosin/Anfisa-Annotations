package ru.processtech.forome.annotation.controller;

import net.minidev.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.processtech.forome.annotation.Service;
import ru.processtech.forome.annotation.controller.utils.ResponseBuilder;
import ru.processtech.forome.annotation.database.entityobject.user.UserEditable;
import ru.processtech.forome.annotation.exception.ExceptionBuilder;
import ru.processtech.forome.annotation.executionqueue.Execution;
import ru.processtech.forome.annotation.executionqueue.ExecutionTransaction;
import ru.processtech.forome.annotation.executionqueue.ResourceProvider;
import ru.processtech.forome.annotation.network.component.UserEditableComponent;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;

/**
 * http://localhost:8095/user/create?login=test&password=test66&session=...
 */
@Controller
@RequestMapping(value = { "/user", "/annotationservice/user" })
public class UserController {

	@RequestMapping(value = { "create" })
	public CompletableFuture<ResponseEntity> create(HttpServletRequest request) {
		Service service = Service.getInstance();

		String sessionId = request.getParameter("session");

		String login = request.getParameter("login");
		String password = request.getParameter("password");

		return service.getExecutionQueue().execute(service, new Execution<Long>() {

			private UserEditableComponent userEditableComponent;

			@Override
			public void prepare(ResourceProvider resources) {
				userEditableComponent = new UserEditableComponent(resources);
			}

			@Override
			public Long execute(ExecutionTransaction transaction) {
				Long requestUserId = service.getNetworkService().sessionService.checkSession(sessionId);
				if (requestUserId == null) {
					throw ExceptionBuilder.buildInvalidCredentialsException();
				}

				UserEditable user = userEditableComponent.create(login, password, transaction);
				return user.getId();
			}
		})
				.thenApply(userId -> {
					JSONObject out = new JSONObject();
					out.put("user_id", userId);
					return out;
				})
				.thenApply(out -> ResponseBuilder.build(out))
				.exceptionally(throwable -> ResponseBuilder.build(throwable));

	}
}
