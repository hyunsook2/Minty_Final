package com.Reboot.Minty.trade.controller;

import com.Reboot.Minty.member.entity.User;
import com.Reboot.Minty.member.service.UserService;
import com.Reboot.Minty.trade.entity.Trade;
import com.Reboot.Minty.trade.service.TradeService;
import com.Reboot.Minty.tradeBoard.service.TradeBoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ScheduleController {

    private final TradeService tradeService;

    private final TradeBoardService tradeBoardService;

    @Autowired
    private UserService userService;

    @Autowired
    public ScheduleController(TradeService tradeService, TradeBoardService tradeBoardService) {
        this.tradeService = tradeService;
        this.tradeBoardService = tradeBoardService;
    }
    @GetMapping("/schedule")
    public String schedule(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Long userId = (Long) session.getAttribute("userId");
        List<Trade> trades = tradeService.getTradeList(userId);
        List<User> users = tradeService.getTradeUsers(trades, userId);

        model.addAttribute("trades", trades);
        model.addAttribute("users", users);
        return "trade/schedule";
    }

}
