import { useState } from "react";
import { Form, required, useLogin, useNotify } from "ra-core";
import type { SubmitHandler, FieldValues } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { TextInput } from "@/components/admin/text-input";
import { Notification } from "@/components/admin/notification";

const GoogleIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
    <path
      fill="#4285F4"
      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.27-4.74 3.27-8.1z"
    />
    <path
      fill="#34A853"
      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23z"
    />
    <path
      fill="#FBBC05"
      d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.18a11 11 0 0 0 0 9.88l3.66-2.84z"
    />
    <path
      fill="#EA4335"
      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1A11 11 0 0 0 2.18 7.06l3.66 2.84C6.71 7.3 9.14 5.38 12 5.38z"
    />
  </svg>
);

/**
 * Login page displayed when authentication is enabled and the user is not authenticated.
 *
 * Automatically shown when an unauthenticated user tries to access a protected route.
 * Handles login via authProvider.login() and displays error notifications on failure.
 *
 * @see {@link https://marmelab.com/shadcn-admin-kit/docs/loginpage LoginPage documentation}
 * @see {@link https://marmelab.com/shadcn-admin-kit/docs/security Security documentation}
 */
export const LoginPage = (props: { redirectTo?: string }) => {
  const { redirectTo } = props;
  const [loading, setLoading] = useState(false);
  const login = useLogin();
  const notify = useNotify();

  const onError = (error: unknown) => {
    setLoading(false);
    const err = error as { message?: string } | string | undefined;
    notify(
      typeof err === "string"
        ? err
        : typeof err === "undefined" || !err.message
          ? "ra.auth.sign_in_error"
          : err.message,
      {
        type: "error",
        messageArgs: {
          _:
            typeof err === "string"
              ? err
              : err && err.message
                ? err.message
                : undefined,
        },
      },
    );
  };

  const handleSubmit: SubmitHandler<FieldValues> = (values) => {
    setLoading(true);
    login(values, redirectTo)
      .then(() => setLoading(false))
      .catch(onError);
  };

  const handleGoogle = () => {
    setLoading(true);
    login({ method: "google" }, redirectTo)
      .then(() => setLoading(false))
      .catch(onError);
  };

  return (
    <div className="min-h-screen flex">
      <div className="container relative grid flex-col items-center justify-center sm:max-w-none lg:grid-cols-2 lg:px-0">
        <div className="relative hidden h-full flex-col bg-muted p-10 text-white dark:border-r lg:flex">
          <div className="absolute inset-0 bg-red-900" />
          <div className="relative z-20 flex items-center text-lg font-medium">
            <span className="mr-2 text-2xl">🥢</span>
            Berlin Chinese Food Map
          </div>
          <div className="relative z-20 mt-auto">
            <blockquote className="space-y-2">
              <p className="text-lg">
                柏林中餐地图 Admin Panel
              </p>
              <p className="text-sm text-zinc-300">
                Community-driven Chinese restaurant guide for Berlin.
              </p>
            </blockquote>
          </div>
        </div>
        <div className="lg:p-8">
          <div className="mx-auto flex w-full flex-col justify-center space-y-6 sm:w-[350px]">
            <div className="flex flex-col space-y-2 text-center">
              <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
              <p className="text-sm leading-none text-muted-foreground">
                Admin access only
              </p>
            </div>
            <Button
              type="button"
              variant="outline"
              className="w-full cursor-pointer"
              disabled={loading}
              onClick={handleGoogle}
            >
              <GoogleIcon className="mr-2 h-4 w-4" />
              Sign in with Google
            </Button>
            <div className="flex items-center gap-2">
              <div className="h-px flex-1 bg-border" />
              <span className="text-xs text-muted-foreground">or</span>
              <div className="h-px flex-1 bg-border" />
            </div>
            <Form className="space-y-8" onSubmit={handleSubmit}>
              <TextInput
                label="Email"
                source="email"
                type="email"
                validate={required()}
              />
              <TextInput
                label="Password"
                source="password"
                type="password"
                validate={required()}
              />
              <Button
                type="submit"
                className="cursor-pointer"
                disabled={loading}
              >
                Sign in
              </Button>
            </Form>
          </div>
        </div>
      </div>
      <Notification />
    </div>
  );
};
