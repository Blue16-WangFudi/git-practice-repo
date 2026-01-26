import flet as ft
try:
    if hasattr(ft, 'Colors'):
        print("SURFACE_VARIANT in Colors:", hasattr(ft.Colors, 'SURFACE_VARIANT'))
        print("PRIMARY in Colors:", hasattr(ft.Colors, 'PRIMARY'))
    
except Exception as e:
    print(e)
