# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - (upcoming changes)

## [0.4.4] - 2025-05-17

### Fixed

- Missing "media container" data in video sources scanned via Forum plugin

### Changed

- Made Forum plugin page scanning asynchronous

## [0.4.3] - 2025-04-04

### Fixed

- Error with VideoStreamLocator relationships when auto-healing from a sanity check
- Bug: Video Source could not be deleted if it had never been streamed
- Bug: VPN status not accurately detected

### Added

- Endpoint for downloading an individual video stream

## [0.4.2] - 2025-01-29

### Fixed

- Extracted FFmpeg additional arguments to settings

## [0.4.1] - 2024-12-30

### Added

- File server user bandwidth monitoring, finished Filefox plugin

### Fixed

- VPN heartbeat at startup
- Dependency validation now creates needed directories if they are missing

## [0.4.0] - 2024-12-26

### Added

- VPN heartbeat (scheduled status check)
- External, editable ffmpeg streaming arguments
- Startup dependency validation

### Changed

- Switched to external ffmpeg-wrapper

### Fixed

- Fixture processing
- Missing audio streams for match videos

## [0.3.5] - 2024-06-11

### Changed

- Migrated application package from self.me.matchday to net.tomasbot.matchday
- Versioned, standardized API (all under /api/<api_version>)

### Fixed

- Video stream status
- Duplicate VideoStreamLocators, causing stream to not delete properly

## [0.3.4] - 2024-05-21

### Added

- Can change log level at runtime
- Auto-heal system based on Sanity Report

## [0.3.3] - 2024-05-15

### Fixed

- Plugin state (enabled/disabled) now saved between application restarts
- Fixed silent transaction rollback when one or more scanned Events is invalid

### Changed

- Moved settings persistence from database to file
- Decoupled settings

## [0.3.1 - 0.3.2] - 2024-03-30

### Added

- Forum datasource plugin

## [0.3.0] - 2023-12-26

### Added

- Add new Match dialog
- Add/edit Video Stream dialog

## [0.2.0] - 2023-11-18

### Added

- Add VPN control
- Add this changelog!