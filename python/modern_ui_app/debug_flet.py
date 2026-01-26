import flet as ft
try:
    print("flet version:", ft.version)
    print("Has colors:", hasattr(ft, 'colors'))
    if hasattr(ft, 'colors'):
        print("flet.colors content sample:", dir(ft.colors)[:5])
    else:
        print("flet dir:", dir(ft))
except Exception as e:
    print("Error:", e)
