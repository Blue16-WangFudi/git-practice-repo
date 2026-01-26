import flet as ft

class Task(ft.Column):
    def __init__(self, task_name, task_delete_callback):
        super().__init__()
        self.task_name = task_name
        self.task_delete_callback = task_delete_callback
        self.edit_name = ft.TextField(expand=1)

        # Task display view (Checkbox + Buttons)
        self.display_task = ft.Checkbox(
            value=False, 
            label=self.task_name, 
            on_change=self.status_changed,
            fill_color=ft.Colors.PRIMARY
        )
        self.edit_view = ft.Row(
            visible=False,
            alignment=ft.MainAxisAlignment.SPACE_BETWEEN,
            vertical_alignment=ft.CrossAxisAlignment.CENTER,
            controls=[
                self.edit_name,
                ft.IconButton(
                    icon=ft.Icons.DONE_OUTLINE_OUTLINED,
                    icon_color=ft.Colors.GREEN,
                    tooltip="Update To-Do",
                    on_click=self.save_clicked,
                ),
            ],
        )

        self.display_view = ft.Container(
            content=ft.Row(
                alignment=ft.MainAxisAlignment.SPACE_BETWEEN,
                vertical_alignment=ft.CrossAxisAlignment.CENTER,
                controls=[
                    self.display_task,
                    ft.Row(
                        spacing=0,
                        controls=[
                            ft.IconButton(
                                icon=ft.Icons.CREATE_OUTLINED,
                                tooltip="Edit To-Do",
                                on_click=self.edit_clicked,
                            ),
                            ft.IconButton(
                                ft.Icons.DELETE_OUTLINE,
                                tooltip="Delete To-Do",
                                on_click=self.delete_clicked,
                                icon_color=ft.Colors.RED_400,
                            ),
                        ],
                    ),
                ],
            ),
            padding=10,
            border_radius=10,
            bgcolor="surfaceVariant", # Use theme color string
            animate=ft.animation.Animation(300, ft.AnimationCurve.DECELERATE),
        )

        self.controls = [self.display_view, self.edit_view]

    def edit_clicked(self, e):
        self.edit_name.value = self.display_task.label
        self.display_view.visible = False
        self.edit_view.visible = True
        self.update()

    def save_clicked(self, e):
        self.display_task.label = self.edit_name.value
        self.display_view.visible = True
        self.edit_view.visible = False
        self.update()

    def status_changed(self, e):
        self.update()

    def delete_clicked(self, e):
        self.task_delete_callback(self)

class TodoApp(ft.Column):
    def __init__(self):
        super().__init__()
        self.new_task = ft.TextField(
            hint_text="What needs to be done?", 
            on_submit=self.add_clicked, 
            expand=True,
            border_radius=20,
            filled=True,
            bgcolor="surfaceVariant"
        )
        self.tasks = ft.Column()

        self.filter = ft.Tabs(
            selected_index=0,
            on_change=self.tabs_changed,
            tabs=[
                ft.Tab(text="all"),
                ft.Tab(text="active"),
                ft.Tab(text="completed"),
            ],
        )

        self.items_left = ft.Text("0 items left")

        # Main Layout
        self.controls = [
            ft.Row(
                controls=[
                    self.new_task,
                    ft.FloatingActionButton(
                        icon=ft.Icons.ADD, 
                        on_click=self.add_clicked,
                        elevation=0
                    ),
                ],
            ),
            ft.Column(
                spacing=25,
                controls=[
                    self.filter,
                    self.tasks,
                    ft.Row(
                        alignment=ft.MainAxisAlignment.SPACE_BETWEEN,
                        vertical_alignment=ft.CrossAxisAlignment.CENTER,
                        controls=[
                            self.items_left,
                            ft.OutlinedButton(
                                text="Clear completed", 
                                on_click=self.clear_clicked
                            ),
                        ],
                    ),
                ],
            ),
        ]

    def add_clicked(self, e):
        if self.new_task.value:
            task = Task(self.new_task.value, self.task_delete)
            self.tasks.controls.append(task)
            self.new_task.value = ""
            self.new_task.focus()
            self.update_async()

    def task_delete(self, task):
        self.tasks.controls.remove(task)
        self.update_async()

    def tabs_changed(self, e):
        self.update_async()

    def clear_clicked(self, e):
        for task in self.tasks.controls[:]:
            if task.display_task.value:
                self.task_delete(task)

    def update_async(self):
        status = self.filter.tabs[self.filter.selected_index].text
        count = 0
        for task in self.tasks.controls:
            task.visible = (
                status == "all"
                or (status == "active" and not task.display_task.value)
                or (status == "completed" and task.display_task.value)
            )
            if not task.display_task.value:
                count += 1
        self.items_left.value = f"{count} active item(s)"
        super().update()

def main(page: ft.Page):
    page.title = "Modern Todo App"
    page.horizontal_alignment = ft.CrossAxisAlignment.CENTER
    page.scroll = ft.ScrollMode.ADAPTIVE
    page.theme = ft.Theme(color_scheme_seed=ft.Colors.INDIGO)
    page.dark_theme = ft.Theme(color_scheme_seed=ft.Colors.INDIGO)
    page.theme_mode = ft.ThemeMode.SYSTEM  # Follow system theme

    def theme_toggle(e):
        page.theme_mode = (
            ft.ThemeMode.DARK
            if page.theme_mode == ft.ThemeMode.LIGHT
            else ft.ThemeMode.LIGHT
        )
        toggle_icon.icon = (
            ft.Icons.DARK_MODE_OUTLINED
            if page.theme_mode == ft.ThemeMode.LIGHT
            else ft.Icons.LIGHT_MODE_OUTLINED
        )
        page.update()

    toggle_icon = ft.IconButton(
        ft.Icons.DARK_MODE_OUTLINED, 
        on_click=theme_toggle,
        tooltip="Toggle Theme"
    )

    page.appbar = ft.AppBar(
        title=ft.Text("My Tasks", weight=ft.FontWeight.BOLD),
        center_title=False,
        bgcolor="surfaceVariant",
        actions=[
            toggle_icon,
            ft.IconButton(ft.Icons.SETTINGS, tooltip="Settings"),
            ft.SizedBox(width=10)
        ],
    )

    # Add the Todo App View
    app = TodoApp()
    
    # Responsive container
    page.add(
        ft.Container(
            content=app,
            width=600, # Limit width for better desktop look
            padding=ft.padding.only(top=20),
        )
    )

if __name__ == "__main__":
    ft.app(target=main)