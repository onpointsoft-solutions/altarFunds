# Logo Placement Guide

## Where to Place Your Logo

Place your church logo file in this directory:

```
src/main/resources/images/
```

## Supported Logo Files

The application will look for logos in this order:

1. **logo.png** (Primary logo file)
2. **sanctum-logo.png** (Fallback logo file)

## Logo Requirements

- **Format**: PNG (recommended for transparency support)
- **Size**: Any size - will be automatically scaled
- **Background**: Transparent or dark background recommended
- **Resolution**: High resolution for best quality

## Logo Usage

Once placed, the logo will automatically appear in:
- LoginFrame (120x120 pixels)
- ChurchAdminFrame (32x32 pixels) 
- TreasurerDashboardFrame (20x20 pixels)
- SecretaryDashboardFrame (20x20 pixels)
- UsherDashboardFrame (20x20 pixels)

## Example

Place your logo file as:
```
src/main/resources/images/logo.png
```

## Fallback

If no logo file is found, the application will display a custom-drawn church icon as fallback.

## Testing

After placing your logo, restart the application to see the changes. The logo will be cached, so you may need to restart to see updates.
