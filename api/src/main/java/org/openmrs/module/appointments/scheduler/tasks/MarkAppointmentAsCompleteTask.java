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

import java.util.ArrayList;
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
         // Retrieve appointments with CheckedIn status
        AppointmentSearchRequest checkedInRequest = new AppointmentSearchRequest();
        checkedInRequest.setStatus(AppointmentStatus.CheckedIn);
        checkedInRequest.setStartDate(DateUtil.getStartOfDay());
        checkedInRequest.setEndDate(DateUtil.getEndOfDay());
        List<Appointment> checkedInAppointments = appointmentsService.search(checkedInRequest);

        /*  Retrieve appointments with Scheduled status for today. 
            This is necessary to check if the patient has visited and had an encounter today
            incases where checkIn status might not have been set yet they visited. We need to mark the appointment as completed
        */
        AppointmentSearchRequest scheduledRequest = new AppointmentSearchRequest();
        scheduledRequest.setStatus(AppointmentStatus.Scheduled);
        scheduledRequest.setStartDate(DateUtil.getStartOfDay());
        scheduledRequest.setEndDate(DateUtil.getEndOfDay());
        List<Appointment> scheduledAppointments = appointmentsService.search(scheduledRequest);

        // Combine both lists
        List<Appointment> appointments = new ArrayList<>();
        appointments.addAll(checkedInAppointments);
        appointments.addAll(scheduledAppointments);

        for (Appointment appointment : appointments) {
            if (hasVisitAndEncounterToday(appointment.getPatient())) {
                String status = AppointmentStatus.Completed.toString();
                appointmentsService.changeStatus(appointment, status, today);

            }

        }
    }

    private boolean isAppointmentCheckedIn(Appointment appointment) {
        return appointment.getStatus().equals(AppointmentStatus.CheckedIn);
    }

    private boolean hasVisitAndEncounterToday(Patient patient) {
        Date startOfDay = DateUtil.getStartOfDay();
        Date endOfDay = DateUtil.getEndOfDay();
        Boolean hasVisitAndEncounter = false;
        VisitService visitService = Context.getVisitService();
        List<Visit> visits = visitService.getVisits(null, Arrays.asList(patient), null, null, startOfDay, null, endOfDay,
                null, null, true, false);
                if(!visits.isEmpty()) {
                    for (Visit visit : visits) {
                        if (!visit.getEncounters().isEmpty() ) {
                            hasVisitAndEncounter = true;
                        }
                    }
                }
                
        return hasVisitAndEncounter;

    }

}
