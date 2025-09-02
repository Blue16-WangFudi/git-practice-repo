public class ScheduleController {
    @PostMapping("/schedule")
    public ApiResponse<ScheduleResponse> schedule(@Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.schedule(request);
        return ApiResponse.success(" schedule success", response);
    }
}
