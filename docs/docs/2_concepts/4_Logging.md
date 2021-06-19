---
parent: Concepts
nav_order: 4
permalink: /concepts/logging.html
title: Logging
---

# Logging

diozero uses the excellent [tinylog](https://tinylog.org/v2/) lightweight nad fast logging framework.
The beauty of tinylog is that it uses a static logger class; this avoids having to instantiate a
logger instance for every single class from which you want to create log entries.
