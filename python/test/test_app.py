import unittest

from gen.demo.TestApp import TestApp as App


class TestApp(unittest.TestCase):
    def test_app(self):
        app = App.get_instance()
        app.start()
        app.stop()
