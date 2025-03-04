package com.example.cio;

import java.io.Serializable;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;

import javax.servlet.http.HttpSession;
import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;

@Named
@SessionScoped
public class SessionInfoBean implements Serializable {
    public String getSessionId() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        HttpSession session = (HttpSession)context.getSession(false);
        if(session == null) {
            return "No session";
        } else {
            return session.getId();
        }
    }

    public String getContextPath() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        return context.getRequestContextPath();
    }
}

