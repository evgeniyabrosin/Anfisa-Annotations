/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.network.transport.jetty;

import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.forome.annotation.controller.utils.ResponseBuilder;
import org.forome.annotation.exception.AnnotatorException;
import org.forome.annotation.exception.ExceptionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ServerErrorHandler extends ErrorHandler {

	private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	public ServerErrorHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
	}

	@Override
	public void doError(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
		try {
			if (response.getStatus() == HttpStatus.NOT_FOUND.value()) {
				ResponseEntity<byte[]> responseEntity = ResponseBuilder.build(ExceptionBuilder.buildInvalidOperation("Support only: /annotate"));
				rewriteResponseEntity(response, responseEntity);
				response.getOutputStream().write(responseEntity.getBody());
			} else if (response.getStatus() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
				ResponseEntity<byte[]> responseEntity = ResponseBuilder.build(ExceptionBuilder.buildServerTimeoutException());
				rewriteResponseEntity(response, responseEntity);
				response.getOutputStream().write(responseEntity.getBody());
			} else if (response.getStatus() >= 400 && response.getStatus() < 500) {
				//Ошибки построения запроса клиентом - игнорируем и прокидываем ответ напрямую
			} else {
				Throwable cause = (Throwable) request.getAttribute(Dispatcher.ERROR_EXCEPTION);

				//Способ страшный, но других механизмов гарантирующий отлов всех неизвестных ошибок не нашел.
				List<Throwable> chainThrowables = ExceptionUtils.getThrowableList(cause);
				if (chainThrowables.size() >= 3
						&& chainThrowables.get(2) instanceof AnnotatorException
				) {
					ResponseEntity<byte[]> responseEntity = ResponseBuilder.build(chainThrowables.get(2));
					rewriteResponseEntity(response, responseEntity);
					response.getOutputStream().write(responseEntity.getBody());
					return;
				} else if (chainThrowables.size() > 4
						&& chainThrowables.get(3) instanceof FileUploadBase.IOFileUploadException
						&& chainThrowables.get(4) instanceof EofException
				) {
					//Игнорируем ошибки вида разрыва соединения во время upload файлв
					return;
				} else if (chainThrowables.size() == 5
						&& chainThrowables.get(3) instanceof FileUploadException
						&& chainThrowables.get(4) instanceof EofException
				) {
					//Если поставить скорость передачи в 1 кб и сразу отменить передачу
					return;
				} else if (chainThrowables.size() == 6
						&& chainThrowables.get(3) instanceof FileUploadException
						&& chainThrowables.get(5) instanceof TimeoutException
				) {
					//Если поставить скорость передачи в 1 кб и через некоторое время отменить передачу
					return;
				}

				String message = "BaseRequest: " + baseRequest.toString() + ", response.status: " + response.getStatus();
				if (cause == null) {
					throw new Exception(message);
				} else {
					throw new Exception(message, cause);
				}
			}
		} catch (Throwable e) {
			uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
		}
	}

	private static void rewriteResponseEntity(HttpServletResponse response, ResponseEntity responseEntity) {
		response.setStatus(responseEntity.getStatusCodeValue());
		responseEntity.getHeaders().forEach((name, values) -> {
			response.setHeader(name, values.get(0));
			for (int i = 1; i < values.size(); i++) {
				response.addHeader(name, values.get(i));
			}
		});
	}
}
