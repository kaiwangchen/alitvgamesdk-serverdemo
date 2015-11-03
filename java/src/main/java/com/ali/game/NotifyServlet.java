package com.ali.game;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ali.game.utils.BaodianConsumptionNotification;

public class NotifyServlet extends HttpServlet {
	/**
	 * Demo AppKey is 23167735
	 */ 
	private static final String  BAODIAN_SECRET = "23af0482e52bfa5073fe97893a8d0d00";
	
	private static final long serialVersionUID = -1342258497370294470L;

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String responseBody;
		try {
			BaodianConsumptionNotification notification = BaodianConsumptionNotification.buildFrom(request);

			if (!notification.verify(BAODIAN_SECRET)) {
				throw new RuntimeException("Illegal signature");
			}
			
			boolean isSuccess = notification.isSuccess();
			String appOrderId = notification.getAppOrderId();

			if (isSuccess) {
				// find the order by appOrderId and verify state
				// find item by appOrderId
				// deliever the item to the player
			}
			else {
				// handle the failure 
			}
			// responseBody = notification.reject("custom message");
			responseBody = notification.accept();
		} catch (IllegalArgumentException e) {
			responseBody = BaodianConsumptionNotification.reject(request, e.getMessage());
		}

 		request.setAttribute("result", responseBody);
		RequestDispatcher dispatcher = request.getRequestDispatcher("result.jsp");
		dispatcher.forward(request, response);
	}
}
