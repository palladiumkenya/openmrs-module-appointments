package org.openmrs.module.appointments.scheduler.tasks;

import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentSearchRequest;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.scheduler.tasks.AbstractTask;

import liquibase.pro.packaged.v;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MarkAppointmentAsCompleteTask extends AbstractTask {

    @Override
    public void execute() {
        AppointmentsService appointmentsService = Context.getService(AppointmentsService.class);
        AdministrationService administrationService = Context.getService(AdministrationService.class);
        GlobalProperty schedulerMarksCompleteProperty = administrationService
                .getGlobalPropertyObject("SchedulerMarksComplete");
        Boolean schedulerMarksComplete = Boolean.valueOf(schedulerMarksCompleteProperty.getPropertyValue());

        if (!schedulerMarksComplete) {
            return;
        }

        Date today = new Date();
        AppointmentSearchRequest appointmentSearchRequest = new AppointmentSearchRequest();
        appointmentSearchRequest.setStatus(AppointmentStatus.CheckedIn);
        appointmentSearchRequest.setStartDate(DateUtil.getStartOfDay());
        appointmentSearchRequest.setEndDate(DateUtil.getEndOfDay());
        List<Appointment> appointments = appointmentsService.search(appointmentSearchRequest);

        for (Appointment appointment : appointments) {
            if (isTodaysVisitCheckedOut(appointment.getPatient())) {
                String status = AppointmentStatus.Completed.toString();
                appointmentsService.changeStatus(appointment, status, today);

            }

        }
    }

    private boolean isAppointmentCheckedIn(Appointment appointment) {
        return appointment.getStatus().equals(AppointmentStatus.CheckedIn);
    }

    private boolean isTodaysVisitCheckedOut(Patient patient) {
        Date startOfDay = DateUtil.getStartOfDay();
        Date endOfDay = DateUtil.getEndOfDay();
        VisitService visitService = Context.getVisitService();
        List<Visit> visits = visitService.getVisits(null, Arrays.asList(patient), null, null, startOfDay, null, null,
                endOfDay, null, true, false);
        return visits.size() > 0;

    }

}
