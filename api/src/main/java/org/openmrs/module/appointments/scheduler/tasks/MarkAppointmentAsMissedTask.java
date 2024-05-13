package org.openmrs.module.appointments.scheduler.tasks;

import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentSearchRequest;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MarkAppointmentAsMissedTask extends AbstractTask {

    @Override
    public void execute() {
        AppointmentsService appointmentsService = Context.getService(AppointmentsService.class);
        AdministrationService administrationService = Context.getService(AdministrationService.class);
        GlobalProperty schedulerMarksMissedProperty = administrationService
                .getGlobalPropertyObject("SchedulerMarksMissed");
        Boolean schedulerMarksMissed = Boolean.valueOf(schedulerMarksMissedProperty.getPropertyValue());
        if (!schedulerMarksMissed) {
            return;
        }
        Date today = new Date();
        // Retrieve all previous scheduled appointments
        List<Appointment> previousAppointmentsMarkedAsScheduled = getAllPreviousScheduledAppointments(
                appointmentsService);
        // Mark previous scheduled appointments as missed
        markAppointmentsAsMissed(appointmentsService, previousAppointmentsMarkedAsScheduled);

        if (isEndOfDay(today)) {
            // Retrieve appointments scheduled for today
            List<Appointment> todayAppointments = appointmentsService
                    .getAllAppointmentsInDateRange(DateUtil.getStartOfDay(), DateUtil.getEndOfDay());
            // Mark appointments scheduled for today as missed
             markAppointmentsAsMissed(appointmentsService, todayAppointments);
        }
    }

    private List<Appointment> getAllPreviousScheduledAppointments(AppointmentsService appointmentsService) {
        AppointmentSearchRequest appointmentSearchRequest = new AppointmentSearchRequest();
        Calendar starDate = Calendar.getInstance();
        starDate.add(Calendar.DAY_OF_MONTH, -7); // Range of 7 days
        appointmentSearchRequest.setStatus(AppointmentStatus.Scheduled);
        appointmentSearchRequest.setStartDate(starDate.getTime());
        appointmentSearchRequest.setEndDate(getEndOfPreviousDay());
        return appointmentsService.search(appointmentSearchRequest);
    }

    private void markAppointmentsAsMissed(AppointmentsService appointmentsService, List<Appointment> appointments) {
        if (appointments != null && !appointments.isEmpty()) {
            Date today = new Date();
            String status = AppointmentStatus.Missed.toString();
            for (Appointment appointment : appointments) {
                 appointmentsService.changeStatus(appointment, status, today);
            }
        }
    }

    private boolean isEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        // Check if the current time is past a certain threshold (e.g., 11:50:50 PM)
        return (hour == 23 && minute == 50 && second == 50);
    }

    private static Date getEndOfPreviousDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));
        return calendar.getTime();
    }
}
